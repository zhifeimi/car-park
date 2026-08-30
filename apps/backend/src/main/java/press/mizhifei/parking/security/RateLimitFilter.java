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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * Per-client-IP rate limiting (bucket4j) on the two abuse-prone anonymous POSTs:
 * {@code POST /auth/login} (brute force) and {@code POST /parking/booking} (booking spam).
 * Exceeding a limit yields HTTP 429 with the standard envelope:
 * {@code {code:429, msg:"请求过于频繁，请稍后再试"}}.
 *
 * <p>In-memory only — correct because the system runs as a single instance (documented; the
 * legacy Redis dependency was deliberately eliminated). Client IP honours the first hop of
 * {@code X-Forwarded-For} (the app sits behind nginx; {@code forward-headers-strategy: native}
 * is set).
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
        if (HttpMethod.POST.matches(req.getMethod())) {
            String path = req.getRequestURI();
            Integer limit = limitFor(path);
            if (limit != null) {
                pruneIfNeeded();
                Bucket bucket = buckets.computeIfAbsent(limitKey(path, clientIp(req)),
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
        }
        chain.doFilter(request, response);
    }

    private Integer limitFor(String path) {
        if ("/auth/login".equals(path)) {
            return loginPerMinute;
        }
        if ("/parking/booking".equals(path)) {
            return bookingPerMinute;
        }
        return null;
    }

    private String limitKey(String path, String ip) {
        return path + ':' + ip;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    /** Crude memory guard: drop all buckets if the map grows without bound. */
    private void pruneIfNeeded() {
        if (buckets.size() > MAX_TRACKED_IPS) {
            buckets.clear();
        }
    }
}
