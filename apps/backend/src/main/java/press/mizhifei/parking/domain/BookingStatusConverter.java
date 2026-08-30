package press.mizhifei.parking.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter for {@link BookingStatus} ↔ {@link Integer} column mapping.
 */
@Converter(autoApply = true)
public class BookingStatusConverter implements AttributeConverter<BookingStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(BookingStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public BookingStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : BookingStatus.fromCode(dbData);
    }
}
