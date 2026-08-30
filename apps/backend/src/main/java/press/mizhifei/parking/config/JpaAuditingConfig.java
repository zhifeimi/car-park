package press.mizhifei.parking.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import press.mizhifei.parking.security.SecurityUtils;

/**
 * Fills the {@code create_by}/{@code update_by} audit columns. Anonymous public writes
 * (citizen H5 bookings) are attributed to {@code "system"}; admin writes to their username.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityUtils.currentUsername()).or(() -> Optional.of("system"));
    }
}
