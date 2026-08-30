package press.mizhifei.parking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security context helpers. Anonymous/public requests (the citizen H5) have no
 * authentication — audit columns for those rows record {@code "system"}
 * (see {@code JpaAuditingConfig}).
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** The logged-in admin username, or {@code null} for anonymous requests. */
    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }
}
