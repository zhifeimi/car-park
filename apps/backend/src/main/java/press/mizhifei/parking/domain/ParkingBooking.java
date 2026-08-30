package press.mizhifei.parking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import press.mizhifei.parking.common.ServiceException;

/**
 * 停车预约记录 — table {@code cp_parking_booking}.
 *
 * <p><b>Join key:</b> {@link #park} stores the lot <em>name</em> (a string, not a foreign-key
 * id) — this is the key used by every availability/statistics query. Renaming a park in the
 * admin console orphans existing bookings from availability counts; the admin UI warns about it.
 *
 * <p>Status semantics: {@code 0} = 已预约 (booked), {@code -1} = 已取消 (cancelled). Cancelling
 * is a partial update that sets {@code status = -1}; rows are never deleted by the public API.
 */
@Entity
@Table(name = "cp_parking_booking")
public class ParkingBooking extends BaseEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 车牌号 */
    @Column(name = "car_plate", length = 20)
    private String carPlate;

    /** 停车场名称 — join key, see class javadoc */
    @Column(name = "park", length = 100)
    private String park;

    /** 预约日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "booking_date")
    private LocalDate bookingDate;

    /** 手机号 */
    @Column(name = "cell_phone", length = 20)
    private String cellPhone;

    /** 状态：0 成功，-1 取消 */
    @Column(name = "status")
    private BookingStatus status;

    /**
     * Cancel an active booking and transition state to {@link BookingStatus#CANCELLED}.
     *
     * @param today current date reference
     * @throws ServiceException if the booking is not in a cancellable state or is for a past date
     */
    public void cancel(LocalDate today) {
        if (this.status == null || !this.status.isCancellable()) {
            throw new ServiceException("该预约记录已取消或已失效");
        }
        if (this.bookingDate != null && today != null && this.bookingDate.isBefore(today)) {
            throw new ServiceException("无法取消已过期的历史预约");
        }
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCarPlate() {
        return carPlate;
    }

    public void setCarPlate(String carPlate) {
        this.carPlate = carPlate;
    }

    public String getPark() {
        return park;
    }

    public void setPark(String park) {
        this.park = park;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
