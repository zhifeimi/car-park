package press.mizhifei.parking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Shared plumbing for the real-MySQL integration suite ({@code @Tag("db")}).
 * Concrete subclasses boot the application with {@code @SpringBootTest} (Boot 4 dropped
 * the {@code @DataJpaTest} slice, so the full context is the integration-test shape).
 *
 * <p>Credentials never live in test resources: they are read once from
 * {@code secrets/carpark-test.env} at the repo root (KEY=VALUE lines, same convention as
 * the server's {@code ~/.config/carpark/carpark.env}) and injected as
 * {@code spring.datasource.*} system properties before the context boots.
 *
 * <p>Tests run WITHOUT a wrapping transaction: every service call commits in its own
 * transaction, exactly as in production — that is what makes the concurrency races
 * meaningful. The price is explicit row-level cleanup before each test.
 */
@Tag("db")
@ActiveProfiles("test")
public abstract class DbIntegrationTest {

    static {
        loadTestDatasourceSecrets();
    }

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanTables() {
        deleteAllRows();
    }

    /**
     * Also callable from inside a test (e.g. between race rounds). Refuses to wipe anything but a
     * {@code *_test} database: {@code localhost:3306} is also where the dev-DB tunnel lands.
     */
    protected void deleteAllRows() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalStateException("Refusing to DELETE all rows in database '" + database
                    + "': the @Tag(\"db\") suite only runs against a *_test database"
                    + " (DB_URL in secrets/carpark-test.env).");
        }
        jdbc.update("DELETE FROM cp_parking_booking");
        jdbc.update("DELETE FROM cp_park");
    }

    protected JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private static void loadTestDatasourceSecrets() {
        Path envFile = findSecretsFile();
        if (envFile == null) {
            throw new IllegalStateException(
                    "secrets/carpark-test.env not found (searched upward from "
                            + Path.of("").toAbsolutePath() + "). Provision the local test MySQL"
                            + " and its credentials first — see the Commands section of CLAUDE.md."
                            + " The @Tag(\"db\") suite cannot run without it.");
        }
        try {
            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                switch (key) {
                    case "DB_URL" -> System.setProperty("spring.datasource.url", value);
                    case "DB_USERNAME" -> System.setProperty("spring.datasource.username", value);
                    case "DB_PASSWORD" -> System.setProperty("spring.datasource.password", value);
                    default -> {
                        // other keys are not consumed by the test slice
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + envFile, e);
        }
    }

    private static Path findSecretsFile() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 3 && dir != null; i++) {
            Path candidate = dir.resolve("secrets/carpark-test.env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
