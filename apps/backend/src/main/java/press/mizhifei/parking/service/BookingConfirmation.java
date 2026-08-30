package press.mizhifei.parking.service;

import java.time.LocalDate;

/**
 * Immutable confirmation emitted by the intake module upon atomic slot reservation.
 */
public record BookingConfirmation(
        Long id,
        String carPlate,
        String park,
        LocalDate bookingDate,
        String cellPhone
) {}
