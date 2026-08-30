package press.mizhifei.parking.security;

// Jackson 3 (Boot 4): the auto-configured mapper lives in tools.jackson,
// not com.fasterxml (which now only holds the annotations).
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import press.mizhifei.parking.web.envelope.R;

/**
 * Stateless JWT security. The permitAll list is the <b>exact</b> anonymous surface of the
 * deployed citizen H5 — nothing else:
 *
 * <ul>
 *   <li>{@code POST /auth/login} (rate-limited)</li>
 *   <li>{@code GET /parking/park/list}, {@code GET /parking/park/leftLotNum}</li>
 *   <li>{@code GET /parking/booking/list}, {@code GET /parking/booking/last}</li>
 *   <li>{@code POST /parking/booking} (rate-limited), {@code PUT /parking/booking}</li>
 *   <li>{@code GET /actuator/health}</li>
 * </ul>
 *
 * <p>Hardening deviations from the legacy config (which permitted wildcard
 * {@code /parking/park/*} and {@code /parking/booking/*} GETs — exposing
 * {@code statisticInfo} and per-id reads anonymously — and even
 * {@code PUT /parking/park}): every endpoint outside the list above now requires a token.
 * Swagger/springdoc (dev profile only) and all monitor consoles require authentication —
 * nothing of that kind is anonymously reachable.
 *
 * <p>The exact boundary is pinned by {@code AnonymousSurfaceMatrixTest} (default suite):
 * adding a permitAll entry or a new public endpoint fails that test unless its
 * expected-public set is updated in the same commit.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/parking/park/list",
                                "/parking/park/leftLotNum",
                                "/parking/booking/list",
                                "/parking/booking/last").permitAll()
                        .requestMatchers(HttpMethod.POST, "/parking/booking").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/parking/booking").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED,
                                        R.fail(R.UNAUTHORIZED, "认证失败，无法访问系统资源")))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(objectMapper, response, HttpServletResponse.SC_FORBIDDEN,
                                        R.fail(HttpServletResponse.SC_FORBIDDEN, "没有权限，请联系管理员"))))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeError(ObjectMapper objectMapper, HttpServletResponse response,
                                   int status, R<Void> body) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
