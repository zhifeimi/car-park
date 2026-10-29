package press.mizhifei.parking.web.envelope;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The legacy shape stays exact; {@code today} appears only where it is set (the park list). */
class TableDataTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void withoutToday_isTheLegacyShape() {
        assertThat(mapper.writeValueAsString(new TableData(0, List.of())))
                .isEqualTo("{\"total\":0,\"rows\":[],\"code\":200,\"msg\":\"查询成功\"}");
    }

    @Test
    void withToday_addsTheParkDate() {
        String json = mapper.writeValueAsString(new TableData(0, List.of()).withToday(LocalDate.of(2026, 10, 29)));
        assertThat(json).contains("\"today\":\"2026-10-29\"");
    }
}
