package press.mizhifei.parking.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import press.mizhifei.parking.service.BookingCommand;
import press.mizhifei.parking.service.BookingConfirmation;
import press.mizhifei.parking.service.BookingService;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingUpdateRequest;

/**
 * Pins the request-integrity contract of the anonymous booking surface: <b>what the citizen
 * submits is what binds</b> — no servlet filter may rewrite the JSON body before Jackson sees
 * it (ADR-005).
 *
 * <p>The retired {@code XssFilter}/{@code HtmlFilter} pair HTML-entity-encoded angle brackets
 * pre-bind, so a remark like {@code 1 < 2} persisted as {@code 1 &lt; 2}. This test posts
 * markup-bearing payloads and asserts, at the {@link BookingService} interface, that the values
 * reaching binding are byte-identical.
 *
 * <p>Runs in the default suite (no {@code @Tag("db")}): the service is spied and stubbed, so no
 * SQL executes — yet the full application context (and every registered servlet filter) is live,
 * which a {@code @WebMvcTest} slice would not load. A future "helpful" sanitizer added below the
 * test seam turns this red.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RequestBodyIntegrityTest {

    /** Markup-bearing strings; byte-identical survival is the contract. */
    private static final String REMARK_WITH_MARKUP = "<script>alert('xss')</script> 1<2 & 3>2";
    private static final String PARK_WITH_MARKUP = "<b>黄村停车场</b>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private BookingService bookingService;

    @Test
    @DisplayName("POST /parking/booking: markup in the body binds verbatim — no pre-bind rewriting")
    void createBody_bindsWithoutMutation() throws Exception {
        AtomicReference<BookingCommand> captured = new AtomicReference<>();
        Mockito.doReturn(new BookingConfirmation(1L, "粤A12345", PARK_WITH_MARKUP, null, "13800138000"))
                .when(bookingService).intake(any());
        Mockito.doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return new BookingConfirmation(1L, "粤A12345", PARK_WITH_MARKUP, null, "13800138000");
        }).when(bookingService).intake(any());

        String payload = """
                {
                  "carPlate": "粤A12345",
                  "park": "%s",
                  "bookingDate": "2030-01-01",
                  "cellPhone": "13800138000",
                  "remark": "%s"
                }
                """.formatted(PARK_WITH_MARKUP, REMARK_WITH_MARKUP);

        MvcResult result = mockMvc.perform(post("/parking/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("intake succeeds against the stubbed service")
                .isEqualTo(200);

        BookingCommand command = captured.get();
        assertThat(command).as("controller must have reached the service").isNotNull();
        assertThat(command.remark())
                .as("remark must bind byte-identical (entity-encoded variants like &lt; or "
                        + "&amp;lt; prove a filter rewrote the body)")
                .isEqualTo(REMARK_WITH_MARKUP);
        assertThat(command.park()).isEqualTo(PARK_WITH_MARKUP);
    }

    @Test
    @DisplayName("PUT /parking/booking: cancel payload binds verbatim — no pre-bind rewriting")
    void cancelBody_bindsWithoutMutation() throws Exception {
        AtomicReference<BookingUpdateRequest> captured = new AtomicReference<>();
        Mockito.doNothing().when(bookingService).update(any());
        Mockito.doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return null;
        }).when(bookingService).update(any());

        String payload = """
                {
                  "id": 42,
                  "status": -1,
                  "remark": "%s"
                }
                """.formatted(REMARK_WITH_MARKUP);

        MvcResult result = mockMvc.perform(put("/parking/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("cancel succeeds against the stubbed service")
                .isEqualTo(200);

        BookingUpdateRequest request = captured.get();
        assertThat(request).as("controller must have reached the service").isNotNull();
        assertThat(request.remark())
                .as("remark must bind byte-identical (entity-encoded variants like &lt; or "
                        + "&amp;lt; prove a filter rewrote the body)")
                .isEqualTo(REMARK_WITH_MARKUP);
    }
}
