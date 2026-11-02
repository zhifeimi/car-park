package press.mizhifei.parking;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Executes the production migration against a simulated pre-country schema on real MySQL. */
@SpringBootTest
class ParkCountryMigrationIntegrationTest extends DbIntegrationTest {

    private static final FileSystemResource MIGRATION = new FileSystemResource(
            Path.of("../../scripts/ops/park-country-migration.sql"));

    @Autowired
    private DataSource dataSource;

    @Test
    void addsCountryBackfillsLegacyRowsAndIsIdempotent() throws Exception {
        // DbIntegrationTest's @BeforeEach has verified the *_test database before any DDL.
        // Keep restoration inside this test: @AfterEach would also run after a failed safety check.
        try {
            jdbcTemplate().update("ALTER TABLE cp_park DROP COLUMN country");
            jdbcTemplate().update("INSERT INTO cp_park (name, lot_num) VALUES ('Legacy lot', 5)");

            runMigration();

            assertThat(countryFor("Legacy lot")).isEqualTo("CN");
            assertThat(jdbcTemplate().queryForObject("""
                    SELECT CONCAT(DATA_TYPE, '(', CHARACTER_MAXIMUM_LENGTH, ')', ':', IS_NULLABLE)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cp_park' AND COLUMN_NAME = 'country'
                    """, String.class)).isEqualTo("char(2):YES");

            jdbcTemplate().update("""
                    INSERT INTO cp_park (name, country, lot_num) VALUES
                    ('Hong Kong lot', 'HK', 5), ('Unassigned lot', NULL, 5)
                    """);
            runMigration();

            assertThat(countryFor("Legacy lot")).isEqualTo("CN");
            assertThat(countryFor("Hong Kong lot")).isEqualTo("HK");
            assertThat(countryFor("Unassigned lot")).isEqualTo("CN");
        } finally {
            runMigration();
        }
    }

    private String countryFor(String name) {
        return jdbcTemplate().queryForObject("SELECT country FROM cp_park WHERE name = ?", String.class, name);
    }

    private void runMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, MIGRATION);
        }
    }
}
