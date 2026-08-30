package press.mizhifei.parking.web.dto;

/**
 * One per-park count row returned by the four statistics queries
 * ({@code {park, booking}} — identical JSON to the legacy {@code ParkBooking}).
 */
public interface ParkBookingStat {

    String getPark();

    Long getBooking();
}
