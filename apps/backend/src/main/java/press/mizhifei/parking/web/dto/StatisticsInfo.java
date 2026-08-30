package press.mizhifei.parking.web.dto;

import java.util.List;

/**
 * Dashboard aggregate — identical JSON shape to the legacy {@code StatisticsInfo}:
 * {@code {todayBooking, weekBooking, monthBooking, yearBooking}}, each a list of
 * {@code {park, booking}}.
 */
public record StatisticsInfo(
        List<ParkBookingStat> todayBooking,
        List<ParkBookingStat> weekBooking,
        List<ParkBookingStat> monthBooking,
        List<ParkBookingStat> yearBooking) {
}
