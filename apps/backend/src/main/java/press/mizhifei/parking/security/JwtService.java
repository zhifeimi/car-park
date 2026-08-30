package press.mizhifei.parking.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stateless HS256 JWT issuing/parsing (jjwt 0.13 API).
 *
 * <p>Deliberate trade-off vs the legacy system: tokens are <b>not</b> stored or refreshed in
 * Redis — they simply expire after {@code app.jwt.expiration-minutes} (default 720). The app
 * is single-instance, so server-side revocation is not needed; expiry is the only lifetime
 * control. The secret must be at least 32 bytes and, in production, comes exclusively from
 * the {@code JWT_SECRET} environment variable (no yaml default — the app fails fast without
 * it).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes:720}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes; set JWT_SECRET (production) or fix application-dev.yml");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String createToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** @return the subject (username), or {@code null} if the token is invalid/expired. */
    public String parseUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
