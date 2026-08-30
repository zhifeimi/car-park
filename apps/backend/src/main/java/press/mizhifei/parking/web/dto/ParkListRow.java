package press.mizhifei.parking.web.dto;

/**
 * One row of {@code GET /parking/park/list} — a {@code cp_park} row plus the computed
 * {@code leftLotNum} (remaining spaces for <em>today</em>: {@code lot_num - count(status=0
 * bookings)}). Interface projection over the native query in {@code ParkRepository}.
 *
 * <p>Wire shape identical to the legacy {@code Park} JSON.
 */
public interface ParkListRow {

    Long getId();

    String getName();

    Long getLotNum();

    /** Computed, never stored: lot_num − today's active bookings for this park. */
    Long getLeftLotNum();

    java.time.LocalDate getOpenDateBegin();

    java.time.LocalDate getOpenDateEnd();

    java.time.LocalTime getOpenTime();

    java.time.LocalTime getCloseTime();

    String getRemark();

    String getCreateBy();

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    java.time.LocalDateTime getCreateTime();

    String getUpdateBy();

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    java.time.LocalDateTime getUpdateTime();
}
