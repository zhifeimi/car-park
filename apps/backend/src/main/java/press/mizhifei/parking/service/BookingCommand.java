package press.mizhifei.parking.service;

import java.time.LocalDate;

/**
 * Immutable command carrying citizen reservation intent to the intake module.
 */
public record BookingCommand(
        String carPlate,
        String park,
        LocalDate bookingDate,
        String cellPhone,
        String remark
) {}
