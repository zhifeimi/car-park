package press.mizhifei.parking.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The bucket key is the container-resolved remote address — a client-supplied
 * {@code X-Forwarded-For} must not buy a fresh bucket — and every abuse-prone anonymous
 * route is limited, cancel and the {@code /last} lookup included.
 */
class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(2, 2);

    private int call(String method, String uri, String remoteAddr, String forwardedFor) throws Exception {
        return call(filter, method, uri, remoteAddr, forwardedFor);
    }

    private static int call(RateLimitFilter filter, String method, String uri, String remoteAddr,
                            String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void rotatingForwardedFor_doesNotEscapeTheLimit() throws Exception {
        assertThat(call("POST", "/auth/login", "203.0.113.7", "10.0.0.1")).isEqualTo(200);
        assertThat(call("POST", "/auth/login", "203.0.113.7", "10.0.0.2")).isEqualTo(200);
        assertThat(call("POST", "/auth/login", "203.0.113.7", "10.0.0.3")).isEqualTo(429);
    }

    @Test
    void distinctClients_haveSeparateBuckets() throws Exception {
        call("POST", "/auth/login", "203.0.113.7", null);
        call("POST", "/auth/login", "203.0.113.7", null);
        assertThat(call("POST", "/auth/login", "203.0.113.7", null)).isEqualTo(429);
        assertThat(call("POST", "/auth/login", "198.51.100.9", null)).isEqualTo(200);
    }

    @Test
    void cancelAndLastLookup_areLimited_otherRoutesAreNot() throws Exception {
        for (String[] route : new String[][] {{"PUT", "/parking/booking"}, {"GET", "/parking/booking/last"}}) {
            call(route[0], route[1], "203.0.113.7", null);
            call(route[0], route[1], "203.0.113.7", null);
            assertThat(call(route[0], route[1], "203.0.113.7", null)).as(route[0] + " " + route[1]).isEqualTo(429);
        }
        for (int i = 0; i < 5; i++) {
            assertThat(call("GET", "/parking/park/list", "203.0.113.7", null)).isEqualTo(200);
        }
    }

    @Test
    void encodedOrDecoratedPaths_shareTheRoutesBucket() throws Exception {
        call("POST", "/auth/login", "203.0.113.7", null);
        call("POST", "/auth/%6Cogin", "203.0.113.7", null);
        for (String spelling : new String[] {"/auth/%6Cogin", "/auth//login", "/auth/login;x=1", "/auth/login/"}) {
            assertThat(call("POST", spelling, "203.0.113.7", null)).as(spelling).isEqualTo(429);
        }
        call("GET", "/parking/booking/%6Cast", "203.0.113.7", null);
        call("GET", "/parking/booking/last", "203.0.113.7", null);
        assertThat(call("GET", "/parking/booking/%6Cast", "203.0.113.7", null)).isEqualTo(429);
    }

    @Test
    void ipv6Clients_areKeyedByTheirSlash64() throws Exception {
        call("POST", "/auth/login", "2001:db8:1:2::1", null);
        call("POST", "/auth/login", "2001:db8:1:2:aaaa:bbbb:cccc:dddd", null);
        assertThat(call("POST", "/auth/login", "2001:db8:1:2::ffff", null)).isEqualTo(429);
        assertThat(call("POST", "/auth/login", "2001:db8:1:3::1", null)).isEqualTo(200);
    }

    @Test
    void clientKey_normalizesAddresses() {
        assertThat(RateLimitFilter.clientKey("203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(RateLimitFilter.clientKey("::ffff:203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(RateLimitFilter.clientKey("2001:0db8:0001:0002:0:0:0:1")).isEqualTo("2001:db8:1:2::/64");
        assertThat(RateLimitFilter.clientKey("not-an-address:x")).isEqualTo("not-an-address:x");
    }

    @Test
    void aFullTable_neverResetsALiveBucket() throws Exception {
        AtomicLong now = new AtomicLong(0);
        RateLimitFilter small = new RateLimitFilter(2, 2, 3, now::get);
        call(small, "POST", "/auth/login", "203.0.113.7", null);
        call(small, "POST", "/auth/login", "203.0.113.7", null);
        // flood the table with fresh keys
        for (int i = 1; i <= 20; i++) {
            call(small, "POST", "/auth/login", "198.51.100." + i, null);
        }
        assertThat(small.trackedClients()).isLessThanOrEqualTo(4); // 3 + one shared overflow bucket
        // the brute-force counter survived the flood
        assertThat(call(small, "POST", "/auth/login", "203.0.113.7", null)).isEqualTo(429);
    }

    @Test
    void idleBuckets_areEvictedOnceTheyHaveRefilled() throws Exception {
        AtomicLong now = new AtomicLong(0);
        RateLimitFilter small = new RateLimitFilter(2, 2, 2, now::get);
        call(small, "POST", "/auth/login", "198.51.100.1", null);
        call(small, "POST", "/auth/login", "198.51.100.2", null);
        now.addAndGet(java.time.Duration.ofMinutes(2).toNanos());
        call(small, "POST", "/auth/login", "198.51.100.3", null);
        assertThat(small.trackedClients()).isEqualTo(1);
    }
}
