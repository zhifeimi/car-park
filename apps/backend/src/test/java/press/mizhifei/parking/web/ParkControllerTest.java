package press.mizhifei.parking.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.repository.UserRepository;
import press.mizhifei.parking.security.JwtService;
import press.mizhifei.parking.service.ParkCriteria;
import press.mizhifei.parking.service.ParkService;
import press.mizhifei.parking.web.dto.ParkListRow;
import press.mizhifei.parking.web.envelope.TableData;

/** Wire-level contract consumed by the H5; no DB is needed because the service is stubbed. */
@SpringBootTest
@AutoConfigureMockMvc
class ParkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkService parkService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String bearer;

    @BeforeEach
    void adminToken() {
        when(userRepository.existsByUsername("test-admin")).thenReturn(true);
        bearer = "Bearer " + jwtService.createToken("test-admin");
    }

    @Test
    void publicList_serializesCountryAndParkToday() throws Exception {
        ParkListRow mainland = new TestParkListRow("Mainland lot", "CN");
        ParkListRow legacy = new TestParkListRow("Legacy lot", null);
        when(parkService.list(any(ParkCriteria.class))).thenReturn(
                new TableData(2, List.of(mainland, legacy)).withToday(LocalDate.of(2026, 11, 2)));

        mockMvc.perform(get("/parking/park/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-11-02"))
                .andExpect(jsonPath("$.rows[0].country").value("CN"))
                .andExpect(jsonPath("$.rows[1].country").hasJsonPath())
                .andExpect(jsonPath("$.rows[1].country").value(nullValue()));
    }

    @Test
    void getPark_serializesCountry() throws Exception {
        Park park = Park.create("Hong Kong lot", "HK", 10L, null, null, null, null, null);
        park.setId(7L);
        when(parkService.get(7L)).thenReturn(park);

        mockMvc.perform(get("/parking/park/7").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.country").value("HK"));
    }

    @Test
    void controllerValidatesCountryOnCreateAndPartialUpdate() throws Exception {
        for (String country : new String[] {null, "", "CHN", "C1"}) {
            String countryField = country == null ? "" : ",\"country\":\"" + country + "\"";
            mockMvc.perform(post("/parking/park").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test park\",\"lotNum\":10" + countryField + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
        mockMvc.perform(put("/parking/park").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"lotNum\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        for (String country : new String[] {"", "CHN", "C1"}) {
            mockMvc.perform(put("/parking/park").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":7,\"country\":\"" + country + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    private static final class TestParkListRow implements ParkListRow {
        private final String name;
        private final String country;

        private TestParkListRow(String name, String country) {
            this.name = name;
            this.country = country;
        }

        public Long getId() { return null; }
        public String getName() { return name; }
        public String getCountry() { return country; }
        public Long getLotNum() { return null; }
        public Long getLeftLotNum() { return null; }
        public LocalDate getOpenDateBegin() { return null; }
        public LocalDate getOpenDateEnd() { return null; }
        public LocalTime getOpenTime() { return null; }
        public LocalTime getCloseTime() { return null; }
        public String getRemark() { return null; }
        public String getCreateBy() { return null; }
        public LocalDateTime getCreateTime() { return null; }
        public String getUpdateBy() { return null; }
        public LocalDateTime getUpdateTime() { return null; }
    }
}
