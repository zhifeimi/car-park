package press.mizhifei.parking.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.springframework.http.MediaType;
import org.springframework.web.util.UrlPathHelper;

/**
 * Per-client-IP rate limiting (bucket4j) on the abuse-prone anonymous endpoints:
 * {@code POST /auth/login} (brute force), {@code POST /parking/booking} (booking spam),
 * {@code PUT /parking/booking} (mass cancel) and {@code GET /parking/booking/last} (lookup
 * enumeration). Exceeding a limit yields HTTP 429 with the standard envelope:
 * {@code {code:429, msg:"请求过于频繁，请稍后再试"}}.
 *
 * <p>In-memory only — correct because the system runs as a single instance (documented; the
 * legacy Redis dependency was deliberately eliminated). The client IP is
 * {@link HttpServletRequest#getRemoteAddr()}: with {@code forward-headers-strategy: native},
 * Tomcat's RemoteIpValve has already resolved it from {@code X-Forwarded-For}, skipping only
 * trusted internal proxies — never the client-controlled first hop, which a caller could
 * rotate for a fresh bucket per request.
 *
 * <p>Three ways around a per-address limit are closed here:
 * <ul>
 *   <li><b>Path spelling.</b> The route is the decoded, normalized path (what Spring Security and
 *       MVC match), not the raw URI — {@code /auth/%6Cogin} or {@code /auth//login;x} is still
 *       {@code /auth/login}.</li>
 *   <li><b>IPv6 rotation.</b> A single subscriber usually holds a whole /64, so IPv6 clients are
 *       keyed by their /64 prefix; IPv4 (and IPv4-mapped IPv6) by the full address.</li>
 *   <li><b>Flushing the table.</b> Eviction only drops buckets idle for at least the refill
 *       period — those are full again, so dropping one equals a fresh bucket and no counter is
 *       ever reset early. If the table is still full of live buckets, new clients share one
 *       overflow bucket per route instead of evicting anyone.</li>
 * </ul>
 */
public class RateLimitFilter implements Filter {

    static final int MAX_TRACKED_CLIENTS = 100_000;
    private static final Duration PERIOD = Duration.ofMinutes(1);
    private static final long SWEEP_INTERVAL_NANOS = Duration.ofSeconds(10).toNanos();
    private static final String OVERFLOW_CLIENT = "overflow";

    private final int loginPerMinute;
    private final int bookingPerMinute;
    private final int maxTrackedClients;
    private final LongSupplier nanoTime;
    private final ConcurrentHashMap<String, Tracked> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastSweep;

    /** A bucket plus when it was last used, for lossless idle eviction. */
    private record Tracked(Bucket bucket, AtomicLong lastUsed) {
    }

    public RateLimitFilter(int loginPerMinute, int bookingPerMinute) {
        this(loginPerMinute, bookingPerMinute, MAX_TRACKED_CLIENTS, System::nanoTime);
    }

    RateLimitFilter(int loginPerMinute, int bookingPerMinute, int maxTrackedClients, LongSupplier nanoTime) {
        this.loginPerMinute = loginPerMinute;
        this.bookingPerMinute = bookingPerMinute;
        this.maxTrackedClients = maxTrackedClients;
        this.nanoTime = nanoTime;
        this.lastSweep = new AtomicLong(nanoTime.getAsLong() - SWEEP_INTERVAL_NANOS);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String route = req.getMethod() + ' ' + routePath(req);
        Integer limit = limitFor(route);
        if (limit != null && !bucketFor(route, clientKey(req.getRemoteAddr()), limit).tryConsume(1)) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(429);
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * The path as the dispatcher sees it: percent-decoded, path parameters ({@code ;...}) and
     * duplicate slashes removed, context path stripped, no trailing slash.
     */
    static String routePath(HttpServletRequest req) {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(req);
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /** IPv6 → its /64 prefix; anything else (IPv4, unparsable) → the address as given. */
    static String clientKey(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.indexOf(':') < 0) {
            return remoteAddr;
        }
        try {
            // a literal: ofLiteral never does a DNS lookup; ::ffff:a.b.c.d comes back as IPv4
            InetAddress address = InetAddress.ofLiteral(remoteAddr);
            if (!(address instanceof Inet6Address)) {
                return address.getHostAddress();
            }
            byte[] b = address.getAddress();
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < 8; i += 2) {
                prefix.append(Integer.toHexString(((b[i] & 0xff) << 8) | (b[i + 1] & 0xff))).append(':');
            }
            return prefix.append(":/64").toString();
        } catch (IllegalArgumentException e) {
            return remoteAddr;
        }
    }

    private Bucket bucketFor(String route, String client, int limit) {
        long now = nanoTime.getAsLong();
        String key = route + ' ' + client;
        Tracked tracked = buckets.get(key);
        if (tracked == null) {
            if (buckets.size() >= maxTrackedClients) {
                sweepIdle(now);
            }
            if (buckets.size() >= maxTrackedClients) {
                // table full of live buckets: share one bucket rather than reset anyone's
                key = route + ' ' + OVERFLOW_CLIENT;
            }
            tracked = buckets.computeIfAbsent(key, k -> new Tracked(newBucket(limit), new AtomicLong(now)));
        }
        tracked.lastUsed().set(now);
        return tracked.bucket();
    }

    /** Drops buckets idle ≥ one refill period (full again, so lossless); at most every 10s. */
    private void sweepIdle(long now) {
        long last = lastSweep.get();
        if (now - last < SWEEP_INTERVAL_NANOS || !lastSweep.compareAndSet(last, now)) {
            return;
        }
        long idleBefore = now - PERIOD.toNanos();
        buckets.values().removeIf(t -> t.lastUsed().get() - idleBefore <= 0);
    }

    private static Bucket newBucket(int limit) {
        return Bucket.builder()
                .addLimit(l -> l.capacity(limit).refillGreedy(limit, PERIOD))
                .build();
    }

    private Integer limitFor(String route) {
        return switch (route) {
            case "POST /auth/login" -> loginPerMinute;
            case "POST /parking/booking", "PUT /parking/booking", "GET /parking/booking/last" -> bookingPerMinute;
            default -> null;
        };
    }

    int trackedClients() {
        return buckets.size();
    }
}
