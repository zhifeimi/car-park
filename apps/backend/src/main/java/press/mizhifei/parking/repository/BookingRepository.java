package press.mizhifei.parking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import press.mizhifei.parking.domain.BookingStatus;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.web.dto.ParkBookingStat;

/**
 * {@code cp_parking_booking} access. The four statistics queries are verbatim ports of
 * {@code ParkingBookingMapper.xml} ({@code selectToday/Week/Month/YearParkBooking}) — the
 * dashboard depends on exactly these ranges and on the {@code {park, booking}} shape.
 */
@Repository
public interface BookingRepository extends JpaRepository<ParkingBooking, Long>,
        JpaSpecificationExecutor<ParkingBooking> {

    /** {@code GET /parking/booking/last} — latest booking for a phone + plate, by create_time. */
    Optional<ParkingBooking> findFirstByCellPhoneAndCarPlateOrderByCreateTimeDesc(String cellPhone, String carPlate);

    /** Returns true if the car plate already has an active booking on the given date. */
    boolean existsByCarPlateAndBookingDateAndStatus(String carPlate, LocalDate bookingDate, BookingStatus status);

    /** Counts active bookings for a park on a given date (used for capacity check). */
    long countByParkAndBookingDateAndStatus(String park, LocalDate bookingDate, BookingStatus status);

    /**
     * Day count: {@code booking_date = date_format(:date,'%Y-%m-%d')} — the {@code date}
     * parameter stays a <b>String</b> on purpose: the dashboard sends
     * {@code yyyy-MM-dd HH:mm:ss} and MySQL parses it. All four queries count only
     * {@code status = 0} rows and group by the park-name join key.
     */
    @Query(value = """
            select cpb.park as park,
                   count(cpb.id) as booking
            from cp_park cp
                     left join cp_parking_booking cpb
                               on cpb.park = cp.name
            where cpb.booking_date = date_format(:date, '%Y-%m-%d')
              and cpb.status = 0
            group by cpb.park""",
            nativeQuery = true)
    List<ParkBookingStat> todayBooking(@Param("date") String date);

    /** Week count, Monday-start: {@code WEEKDAY()} gives 0 for Monday. */
    @Query(value = """
            select cpb.park as park,
                   count(cpb.id) as booking
            from cp_park cp
                     left join cp_parking_booking cpb
                               on cpb.park = cp.name
            where cpb.booking_date
                      between
                      date_add(date_format(:date, '%Y-%m-%d'), interval -WEEKDAY(date_format(:date, '%Y-%m-%d')) day)
                      and
                      date_add(date_add(date_format(:date, '%Y-%m-%d'), interval -WEEKDAY(date_format(:date, '%Y-%m-%d')) day), interval 6 day)
              and cpb.status = 0
            group by cpb.park""",
            nativeQuery = true)
    List<ParkBookingStat> weekBooking(@Param("date") String date);

    /** Month count: 1st ({@code -DAY()+1}) → {@code LAST_DAY}. */
    @Query(value = """
            select cpb.park as park,
                   count(cpb.id) as booking
            from cp_park cp
                     left join cp_parking_booking cpb
                               on cpb.park = cp.name
            where cpb.booking_date
                      between
                      date_add(date_format(:date, '%Y-%m-%d'), interval -DAY(date_format(:date, '%Y-%m-%d'))+1 DAY)
                      and
                      LAST_DAY(date_format(:date, '%Y-%m-%d'))
              and cpb.status = 0
            group by cpb.park""",
            nativeQuery = true)
    List<ParkBookingStat> monthBooking(@Param("date") String date);

    /** Year count: Jan 1 → Dec 31 ({@code last_day(... + INTERVAL 12-MONTH MONTH)}). */
    @Query(value = """
            select cpb.park as park,
                   count(cpb.id) as booking
            from cp_park cp
                     left join cp_parking_booking cpb
                               on cpb.park = cp.name
            where cpb.booking_date
                      between
                      date_format(:date, '%Y-01-01')
                      and
                      last_day(date_add(date_format(:date, '%Y-%m-%d'), INTERVAL 12-MONTH(date_format(:date, '%Y-%m-%d')) MONTH))
              and cpb.status = 0
            group by cpb.park""",
            nativeQuery = true)
    List<ParkBookingStat> yearBooking(@Param("date") String date);
}
