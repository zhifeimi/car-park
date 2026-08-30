package press.mizhifei.parking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BookingStatusConverterTest {

    private final BookingStatusConverter converter = new BookingStatusConverter();

    @Test
    void convertToDatabaseColumn_mapsToIntegerCode() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(BookingStatus.ACTIVE)).isEqualTo(0);
        assertThat(converter.convertToDatabaseColumn(BookingStatus.CANCELLED)).isEqualTo(-1);
    }

    @Test
    void convertToEntityAttribute_mapsToBookingStatus() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute(0)).isEqualTo(BookingStatus.ACTIVE);
        assertThat(converter.convertToEntityAttribute(-1)).isEqualTo(BookingStatus.CANCELLED);
    }
}
