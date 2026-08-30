package press.mizhifei.parking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Pins the anonymous surface — the exact set of routes the citizen H5 may reach without a
 * token — against the <em>live route registry</em>, so the boundary cannot drift silently:
 *
 * <ul>
 *   <li>every {@code @RequestMapping} route is discovered from {@link RequestMappingHandlerMapping};
 *       a route added to a controller later is automatically enrolled here;</li>
 *   <li>routes in the expected-public set must <b>never</b> answer 401/403 anonymously;</li>
 *   <li>every other route must answer <b>401</b> anonymously;</li>
 *   <li>the expected-public set is restated here independently of {@code SecurityConfig} — the
 *       test is an oracle: editing the config's permitAll list (or deleting a public endpoint)
 *       fails this test unless this set is consciously updated to match.</li>
 * </ul>
 *
 * <p>Runs in the default suite (no {@code @Tag("db")}): the assertions are about the security
 * chain, not the database. Public routes are hit without params/body, so with or without a
 * live MySQL they resolve to a 200 envelope (business failure) — what is asserted is only
 * that the security layer let them through. {@code GET /actuator/health} is not registered in
 * {@code RequestMappingHandlerMapping} (actuator has its own handler mapping), so it gets its
 * own explicit assertion.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnonymousSurfaceMatrixTest {

    /** The anonymous surface of the deployed citizen H5 — exactly these seven, nothing else. */
    private static final Set<PublicRoute> EXPECTED_PUBLIC = Set.of(
            new PublicRoute(HttpMethod.POST, "/auth/login"),
            new PublicRoute(HttpMethod.GET, "/parking/park/list"),
            new PublicRoute(HttpMethod.GET, "/parking/park/leftLotNum"),
            new PublicRoute(HttpMethod.GET, "/parking/booking/list"),
            new PublicRoute(HttpMethod.GET, "/parking/booking/last"),
            new PublicRoute(HttpMethod.POST, "/parking/booking"),
            new PublicRoute(HttpMethod.PUT, "/parking/booking"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private record PublicRoute(HttpMethod method, String path) {
    }

    @Test
    @DisplayName("route matrix: the seven public routes pass anonymously; every other route answers 401")
    void anonymousSurface_matchesExpectedSetExactly() throws Exception {
        List<String> violations = new ArrayList<>();
        Set<PublicRoute> discoveredPublic = new LinkedHashSet<>();

        for (RequestMappingInfo info : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            Set<String> patterns = patternsOf(info);
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                // method-unrestricted mapping — GET is the representative probe
                methods = Set.of(RequestMethod.GET);
            }
            for (String pattern : patterns) {
                for (RequestMethod method : methods) {
                    HttpMethod httpMethod = HttpMethod.valueOf(method.name());
                    PublicRoute route = new PublicRoute(httpMethod, pattern);
                    int status = probe(httpMethod, concreteUrl(pattern));
                    if (EXPECTED_PUBLIC.contains(route)) {
                        discoveredPublic.add(route);
                        if (status == 401 || status == 403) {
                            violations.add("PUBLIC route answered " + status + ": "
                                    + httpMethod + " " + pattern + " (citizen H5 would break)");
                        }
                    } else if (status != 401) {
                        violations.add("PROTECTED route answered " + status + " anonymously: "
                                + httpMethod + " " + pattern + " (expected 401 — check SecurityConfig)");
                    }
                }
            }
        }

        Set<PublicRoute> missing = new LinkedHashSet<>(EXPECTED_PUBLIC);
        missing.removeAll(discoveredPublic);
        for (PublicRoute stale : missing) {
            violations.add("Expected-public route not found in the live registry: "
                    + stale.method() + " " + stale.path() + " (renamed? deleted? update the oracle set)");
        }

        assertThat(violations)
                .as("anonymous surface drift — every violation above is a route whose public/protected\n"
                        + "classification disagrees with the frozen contract. If the change is deliberate,\n"
                        + "update EXPECTED_PUBLIC and SecurityConfig together, and say so in the commit.")
                .isEmpty();
    }

    @Test
    @DisplayName("GET /actuator/health is anonymously reachable (monitoring probe; never 401/403)")
    void actuatorHealth_anonymouslyReachable() throws Exception {
        int status = probe(HttpMethod.GET, "/actuator/health");
        assertThat(status)
                .as("actuator health must stay anonymous for probes (may be 503 when the DB is down — "
                        + "what must never happen is 401/403)")
                .isNotIn(401, 403);
    }

    /** Anonymous request: no Authorization header, no params, no body. */
    private int probe(HttpMethod method, String url) throws Exception {
        MvcResult result = mockMvc.perform(request(method, url)).andReturn();
        return result.getResponse().getStatus();
    }

    private static Set<String> patternsOf(RequestMappingInfo info) {
        Set<String> patterns = new LinkedHashSet<>();
        if (info.getPathPatternsCondition() != null) {
            patterns.addAll(info.getPathPatternsCondition().getPatternValues());
        } else if (info.getPatternsCondition() != null) {
            patterns.addAll(info.getPatternsCondition().getPatterns());
        }
        return patterns;
    }

    /** {@code /parking/booking/{id}} → {@code /parking/booking/1} for probing. */
    private static String concreteUrl(String pattern) {
        return pattern.replaceAll("\\{[^}]+\\}", "1");
    }
}
