package press.mizhifei.parking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JpaAuditingConfigTest {

    @Test
    void ianaZone_isAccepted() {
        assertThat(JpaAuditingConfig.parkZone("Asia/Shanghai")).isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThat(JpaAuditingConfig.parkZone(" Europe/Oslo ")).isEqualTo(ZoneId.of("Europe/Oslo"));
    }

    @Test
    void posixTzValues_failWithThePropertyName() {
        // valid for libc's TZ, not for ZoneId — the reason the park zone is not read from TZ
        for (String posix : new String[] {":/etc/localtime", "CST-8"}) {
            assertThatThrownBy(() -> JpaAuditingConfig.parkZone(posix))
                    .as(posix)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PARK_TIME_ZONE");
        }
    }
}
