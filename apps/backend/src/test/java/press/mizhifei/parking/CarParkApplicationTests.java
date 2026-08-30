package press.mizhifei.parking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context-load smoke test. The datasource is lazy (Hikari opens connections on first use,
 * {@code ddl-auto=none} never touches the schema), so this passes without a running MySQL —
 * it verifies bean wiring: security chain, JWT, auditing, controllers, filters.
 */
@SpringBootTest
class CarParkApplicationTests {

    @Test
    void contextLoads() {
        // application context assembles
    }
}
