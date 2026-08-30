package press.mizhifei.parking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import press.mizhifei.parking.common.ServiceException;

class ParkingBookingTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Test
    void cancel_activeFutureBooking_transitionsToCancelled() {
        ParkingBooking booking = new ParkingBooking();
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setBookingDate(TOMORROW);

        booking.cancel(TODAY);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_activeTodayBooking_transitionsToCancelled() {
        ParkingBooking booking = new ParkingBooking();
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setBookingDate(TODAY);

        booking.cancel(TODAY);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_nullStatus_throwsServiceException() {
        ParkingBooking booking = new ParkingBooking();
        booking.setStatus(null);
        booking.setBookingDate(TOMORROW);

        assertThatThrownBy(() -> booking.cancel(TODAY))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该预约记录已取消或已失效");
    }

    @Test
    void cancel_alreadyCancelled_throwsServiceException() {
        ParkingBooking booking = new ParkingBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setBookingDate(TOMORROW);

        assertThatThrownBy(() -> booking.cancel(TODAY))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该预约记录已取消或已失效");
    }

    @Test
    void cancel_pastDate_throwsServiceException() {
        ParkingBooking booking = new ParkingBooking();
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setBookingDate(YESTERDAY);

        assertThatThrownBy(() -> booking.cancel(TODAY))
                .isInstanceOf(ServiceException.class)
                .hasMessage("无法取消已过期的历史预约");
    }
}
