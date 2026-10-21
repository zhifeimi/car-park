package press.mizhifei.parking.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import press.mizhifei.parking.security.SecurityUtils;

/**
 * Fills the {@code create_by}/{@code update_by} audit columns. Anonymous public writes
 * (citizen H5 bookings) are attributed to {@code "system"}; admin writes to their username.
 *
 * <p>Also owns the park's clock: one {@link Clock} in the {@code spring.jackson.time-zone} zone
 * ({@code ${TZ:...}}), used for "today" checks and the audit timestamps alike — so the host's
 * default zone (UTC in most containers) never decides which day it is at the park.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "parkDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public Clock parkClock(@Value("${spring.jackson.time-zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public DateTimeProvider parkDateTimeProvider(Clock parkClock) {
        return () -> Optional.of(LocalDateTime.now(parkClock));
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityUtils.currentUsername()).or(() -> Optional.of("system"));
    }
}
