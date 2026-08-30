package press.mizhifei.parking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import press.mizhifei.parking.security.JwtAuthenticationFilter;
import press.mizhifei.parking.security.RateLimitFilter;

/**
 * Servlet filter wiring and ordering (lower runs first):
 *
 * <ol>
 *   <li>{@link RateLimitFilter} ({@code HIGHEST+10}) — reject floods before any work;</li>
 *   <li>Spring Security chain (default order), which internally runs
 *       {@link JwtAuthenticationFilter}.</li>
 * </ol>
 *
 * There is deliberately <b>no</b> request-body sanitizing filter (ADR-005): what the client
 * submits binds verbatim, and XSS defence lives at DTO validation plus output-side escaping.
 *
 * The {@code JwtAuthenticationFilter} is a {@code @Component} (for constructor injection),
 * so Boot would auto-register it as a servlet filter and it would run twice — the disabled
 * registration below suppresses that; it only runs inside the security chain.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            @Value("${app.rate-limit.login-per-minute:5}") int loginPerMinute,
            @Value("${app.rate-limit.booking-per-minute:10}") int bookingPerMinute) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(loginPerMinute, bookingPerMinute));
        registration.addUrlPatterns("/*");
        registration.setName("rateLimitFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
