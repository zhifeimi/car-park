package press.mizhifei.parking.security;

import static org.assertj.core.api.Assertions.assertThat;

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
}
