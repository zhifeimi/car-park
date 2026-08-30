package press.mizhifei.parking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import press.mizhifei.parking.common.ServiceException;

class BookingStatusTest {

    @Test
    void activeStatus_propertiesAndPredicates() {
        BookingStatus status = BookingStatus.ACTIVE;
        assertThat(status.getCode()).isEqualTo(0);
        assertThat(status.getLabel()).isEqualTo("预约成功");
        assertThat(status.isActive()).isTrue();
        assertThat(status.isCancellable()).isTrue();
    }

    @Test
    void cancelledStatus_propertiesAndPredicates() {
        BookingStatus status = BookingStatus.CANCELLED;
        assertThat(status.getCode()).isEqualTo(-1);
        assertThat(status.getLabel()).isEqualTo("已取消");
        assertThat(status.isActive()).isFalse();
        assertThat(status.isCancellable()).isFalse();
    }

    @Test
    void fromCode_int_returnsCorrectEnum() {
        assertThat(BookingStatus.fromCode(0)).isEqualTo(BookingStatus.ACTIVE);
        assertThat(BookingStatus.fromCode(-1)).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void fromCode_unknownInt_throwsServiceException() {
        assertThatThrownBy(() -> BookingStatus.fromCode(99))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未知预约状态");
    }

    @Test
    void fromCode_object_handlesNullNumbersAndStrings() {
        assertThat(BookingStatus.fromCode(null)).isNull();
        assertThat(BookingStatus.fromCode("")).isNull();
        assertThat(BookingStatus.fromCode("   ")).isNull();
        assertThat(BookingStatus.fromCode(0)).isEqualTo(BookingStatus.ACTIVE);
        assertThat(BookingStatus.fromCode(-1)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(BookingStatus.fromCode("0")).isEqualTo(BookingStatus.ACTIVE);
        assertThat(BookingStatus.fromCode("-1")).isEqualTo(BookingStatus.CANCELLED);
        assertThat(BookingStatus.fromCode("ACTIVE")).isEqualTo(BookingStatus.ACTIVE);
        assertThat(BookingStatus.fromCode("CANCELLED")).isEqualTo(BookingStatus.CANCELLED);
        assertThat(BookingStatus.fromCode("预约成功")).isEqualTo(BookingStatus.ACTIVE);
        assertThat(BookingStatus.fromCode("已取消")).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void fromCode_object_invalid_throwsServiceException() {
        assertThatThrownBy(() -> BookingStatus.fromCode("invalid_status"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("未知预约状态");
    }
}
