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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;

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
 */
public class RateLimitFilter implements Filter {

    private static final int MAX_TRACKED_IPS = 100_000;

    private final int loginPerMinute;
    private final int bookingPerMinute;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(int loginPerMinute, int bookingPerMinute) {
        this.loginPerMinute = loginPerMinute;
        this.bookingPerMinute = bookingPerMinute;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String route = req.getMethod() + ' ' + req.getRequestURI();
        Integer limit = limitFor(route);
        if (limit != null) {
            pruneIfNeeded();
            Bucket bucket = buckets.computeIfAbsent(route + ' ' + req.getRemoteAddr(),
                    k -> Bucket.builder()
                            .addLimit(l -> l.capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)))
                            .build());
            if (!bucket.tryConsume(1)) {
                HttpServletResponse resp = (HttpServletResponse) response;
                resp.setStatus(429);
                resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                resp.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Integer limitFor(String route) {
        return switch (route) {
            case "POST /auth/login" -> loginPerMinute;
            case "POST /parking/booking", "PUT /parking/booking", "GET /parking/booking/last" -> bookingPerMinute;
            default -> null;
        };
    }

    /** Crude memory guard: drop all buckets if the map grows without bound. */
    private void pruneIfNeeded() {
        if (buckets.size() > MAX_TRACKED_IPS) {
            buckets.clear();
        }
    }
}
