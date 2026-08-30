package press.mizhifei.parking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import press.mizhifei.parking.common.ServiceException;

/**
 * Unit tests for {@link Park} domain entity invariants:
 * schedule chronology, capacity non-negativity, and partial update state merging.
 */
class ParkTest {

    private static final String VALID_NAME = "黄麻社区东停车场";
    private static final Long VALID_LOT_NUM = 50L;
    private static final LocalDate DATE_BEGIN = LocalDate.of(2026, 9, 1);
    private static final LocalDate DATE_END = LocalDate.of(2026, 12, 31);
    private static final LocalTime TIME_OPEN = LocalTime.of(8, 0, 0);
    private static final LocalTime TIME_CLOSE = LocalTime.of(22, 0, 0);

    @Test
    void create_validParameters_createsParkSuccessfully() {
        Park park = Park.create(
                VALID_NAME,
                VALID_LOT_NUM,
                DATE_BEGIN,
                DATE_END,
                TIME_OPEN,
                TIME_CLOSE,
                "测试备注"
        );

        assertThat(park.getName()).isEqualTo(VALID_NAME);
        assertThat(park.getLotNum()).isEqualTo(VALID_LOT_NUM);
        assertThat(park.getOpenDateBegin()).isEqualTo(DATE_BEGIN);
        assertThat(park.getOpenDateEnd()).isEqualTo(DATE_END);
        assertThat(park.getOpenTime()).isEqualTo(TIME_OPEN);
        assertThat(park.getCloseTime()).isEqualTo(TIME_CLOSE);
        assertThat(park.getRemark()).isEqualTo("测试备注");
    }

    @Test
    void create_nullDatesAndTimes_createsParkSuccessfully() {
        Park park = Park.create(VALID_NAME, VALID_LOT_NUM, null, null, null, null, null);

        assertThat(park.getName()).isEqualTo(VALID_NAME);
        assertThat(park.getOpenDateBegin()).isNull();
        assertThat(park.getOpenDateEnd()).isNull();
        assertThat(park.getOpenTime()).isNull();
        assertThat(park.getCloseTime()).isNull();
    }

    @Test
    void create_singleDayOpening_permitsEqualDates() {
        Park park = Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, DATE_BEGIN, TIME_OPEN, TIME_CLOSE, null);

        assertThat(park.getOpenDateBegin()).isEqualTo(DATE_BEGIN);
        assertThat(park.getOpenDateEnd()).isEqualTo(DATE_BEGIN);
    }

    @Test
    void create_blankOrNullName_throwsServiceException() {
        assertThatThrownBy(() -> Park.create(null, VALID_LOT_NUM, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场名称不能为空");

        assertThatThrownBy(() -> Park.create("   ", VALID_LOT_NUM, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场名称不能为空");
    }

    @Test
    void create_negativeLotNum_throwsServiceException() {
        assertThatThrownBy(() -> Park.create(VALID_NAME, -1L, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("可预约车位总数不能为负数");
    }

    @Test
    void create_openDateBeginAfterOpenDateEnd_throwsServiceException() {
        LocalDate laterBegin = LocalDate.of(2026, 10, 1);
        LocalDate earlierEnd = LocalDate.of(2026, 9, 1);

        assertThatThrownBy(() -> Park.create(VALID_NAME, VALID_LOT_NUM, laterBegin, earlierEnd, TIME_OPEN, TIME_CLOSE, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("可预约起始日期不能晚于结束日期");
    }

    @Test
    void create_openTimeAfterCloseTime_throwsServiceException() {
        LocalTime laterOpen = LocalTime.of(23, 0, 0);
        LocalTime earlierClose = LocalTime.of(8, 0, 0);

        assertThatThrownBy(() -> Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, DATE_END, laterOpen, earlierClose, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("每天开放时间必须早于关闭时间");
    }

    @Test
    void create_equalOpenAndCloseTime_throwsServiceException() {
        LocalTime sameTime = LocalTime.of(8, 0, 0);

        assertThatThrownBy(() -> Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, DATE_END, sameTime, sameTime, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("每天开放时间必须早于关闭时间");
    }

    // ─── Partial Update Invariant Tests ───────────────────────────────────────

    @Test
    void applyPartialUpdate_validSubsetOfFields_updatesOnlyProvidedFields() {
        Park park = Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, "原备注");

        park.applyPartialUpdate("新车场名称", 100L, null, null, null, null, "新备注");

        assertThat(park.getName()).isEqualTo("新车场名称");
        assertThat(park.getLotNum()).isEqualTo(100L);
        assertThat(park.getOpenDateBegin()).isEqualTo(DATE_BEGIN);
        assertThat(park.getOpenDateEnd()).isEqualTo(DATE_END);
        assertThat(park.getOpenTime()).isEqualTo(TIME_OPEN);
        assertThat(park.getCloseTime()).isEqualTo(TIME_CLOSE);
        assertThat(park.getRemark()).isEqualTo("新备注");
    }

    @Test
    void applyPartialUpdate_newBeginDateExceedsExistingEndDate_throwsServiceException() {
        Park park = Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, LocalDate.of(2026, 9, 30), TIME_OPEN, TIME_CLOSE, null);

        LocalDate invalidNewBegin = LocalDate.of(2026, 10, 15);
        assertThatThrownBy(() -> park.applyPartialUpdate(null, null, invalidNewBegin, null, null, null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("可预约起始日期不能晚于结束日期");
    }

    @Test
    void applyPartialUpdate_newCloseTimePrecedesExistingOpenTime_throwsServiceException() {
        Park park = Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, DATE_END, LocalTime.of(9, 0, 0), LocalTime.of(18, 0, 0), null);

        LocalTime invalidNewClose = LocalTime.of(8, 0, 0);
        assertThatThrownBy(() -> park.applyPartialUpdate(null, null, null, null, null, invalidNewClose, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("每天开放时间必须早于关闭时间");
    }

    @Test
    void applyPartialUpdate_blankName_throwsServiceException() {
        Park park = Park.create(VALID_NAME, VALID_LOT_NUM, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, null);

        assertThatThrownBy(() -> park.applyPartialUpdate("   ", null, null, null, null, null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场名称不能为空");
    }
}
