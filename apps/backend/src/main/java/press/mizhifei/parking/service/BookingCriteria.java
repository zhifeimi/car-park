package press.mizhifei.parking.service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.BookingStatus;
import press.mizhifei.parking.domain.ParkingBooking;

/**
 * 停车预约查询条件 (Booking Query Criteria).
 * <p>
 * Unified query criteria object consolidating search filters, date format validation,
 * pagination bounds, and JPA specification creation across Controller, Service, and Repository seams.
 */
public record BookingCriteria(
        String carPlate,
        String park,
        String bookingDate,
        String cellPhone,
        BookingStatus status,
        Integer pageNum,
        Integer pageSize
) {

    /** Legacy fixed order: {@code booking_date desc, create_time desc, cell_phone, create_by, car_plate}. */
    public static final Sort LEGACY_ORDER = Sort.by(
            Sort.Order.desc("bookingDate"),
            Sort.Order.desc("createTime"),
            Sort.Order.asc("cellPhone"),
            Sort.Order.asc("createBy"),
            Sort.Order.asc("carPlate"));

    public BookingCriteria {
        carPlate = blankToNull(carPlate);
        park = blankToNull(park);
        cellPhone = blankToNull(cellPhone);
        bookingDate = blankToNull(bookingDate);
        if (bookingDate != null) {
            parseDate(bookingDate); // Eagerly validate date format
        }
    }

    /**
     * Factory for an empty unpaged criteria matching all records.
     */
    public static BookingCriteria all() {
        return new BookingCriteria(null, null, null, null, null, null, null);
    }

    /**
     * Factory for unpaged criteria with specific filters.
     */
    public static BookingCriteria unpaged(String carPlate, String park, String bookingDate,
                                          String cellPhone, BookingStatus status) {
        return new BookingCriteria(carPlate, park, bookingDate, cellPhone, status, null, null);
    }

    /**
     * Returns the parsed {@link LocalDate} representation of {@code bookingDate}, or {@code null} if absent.
     */
    public LocalDate parsedBookingDate() {
        return bookingDate == null ? null : parseDate(bookingDate);
    }

    /**
     * Constructs a Spring Data JPA {@link Specification} representing the criteria filters.
     */
    public Specification<ParkingBooking> toSpecification() {
        LocalDate date = parsedBookingDate();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (carPlate != null) {
                predicates.add(cb.equal(root.get("carPlate"), carPlate));
            }
            if (park != null) {
                predicates.add(cb.equal(root.get("park"), park));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("bookingDate"), date));
            }
            if (cellPhone != null) {
                predicates.add(cb.equal(root.get("cellPhone"), cellPhone));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Constructs a {@link Pageable} with legacy sorting from the criteria parameters.
     * Returns {@link Pageable#unpaged(Sort)} if pagination is omitted or non-positive.
     */
    public Pageable toPageable() {
        return isPaged()
                ? PageRequest.of(pageNum - 1, pageSize, LEGACY_ORDER)
                : Pageable.unpaged(LEGACY_ORDER);
    }

    public boolean isPaged() {
        return pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0;
    }

    private static LocalDate parseDate(String bookingDate) {
        try {
            return LocalDate.parse(bookingDate.trim());
        } catch (DateTimeParseException e) {
            throw new ServiceException("预约日期格式错误，应为 yyyy-MM-dd");
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
