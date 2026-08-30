package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.BookingStatus;

/**
 * Unit tests for {@link BookingCriteria} query object:
 * blank-to-null string normalization, date format validation, and pageable translation.
 */
class BookingCriteriaTest {

    @Test
    @DisplayName("normalizes blank strings to null and trims values")
    void compactConstructor_normalizesBlankStringsToNullAndTrims() {
        BookingCriteria criteria = new BookingCriteria(
                "  粤A12345  ",
                "  黄麻停车场  ",
                "  2026-08-20  ",
                "  13800138000  ",
                BookingStatus.ACTIVE,
                1,
                10
        );

        assertThat(criteria.carPlate()).isEqualTo("粤A12345");
        assertThat(criteria.park()).isEqualTo("黄麻停车场");
        assertThat(criteria.bookingDate()).isEqualTo("2026-08-20");
        assertThat(criteria.cellPhone()).isEqualTo("13800138000");
        assertThat(criteria.status()).isEqualTo(BookingStatus.ACTIVE);
        assertThat(criteria.parsedBookingDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(criteria.isPaged()).isTrue();
    }

    @Test
    @DisplayName("blank bookingDate becomes null")
    void compactConstructor_blankDateBecomesNull() {
        BookingCriteria criteria = new BookingCriteria("   ", "", "   ", null, null, null, null);

        assertThat(criteria.carPlate()).isNull();
        assertThat(criteria.park()).isNull();
        assertThat(criteria.bookingDate()).isNull();
        assertThat(criteria.cellPhone()).isNull();
        assertThat(criteria.parsedBookingDate()).isNull();
        assertThat(criteria.isPaged()).isFalse();
    }

    @Test
    @DisplayName("invalid date format in constructor throws ServiceException")
    void compactConstructor_invalidDateFormat_throwsServiceException() {
        assertThatThrownBy(() -> new BookingCriteria(null, null, "invalid-date", null, null, null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约日期格式错误，应为 yyyy-MM-dd");

        assertThatThrownBy(() -> new BookingCriteria(null, null, "2026/08/20", null, null, null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约日期格式错误，应为 yyyy-MM-dd");
    }

    @Test
    @DisplayName("toPageable with valid pageNum and pageSize returns PageRequest with LEGACY_ORDER")
    void toPageable_validPagination_returnsPageRequestWithLegacyOrder() {
        BookingCriteria criteria = new BookingCriteria(null, null, null, null, null, 2, 15);

        Pageable pageable = criteria.toPageable();

        assertThat(pageable.isPaged()).isTrue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort()).isEqualTo(BookingCriteria.LEGACY_ORDER);
        assertThat(pageable).isEqualTo(PageRequest.of(1, 15, BookingCriteria.LEGACY_ORDER));
    }

    @Test
    @DisplayName("toPageable with null or non-positive pagination returns Unpaged with LEGACY_ORDER")
    void toPageable_nullOrNonPositivePagination_returnsUnpagedWithLegacyOrder() {
        assertThat(new BookingCriteria(null, null, null, null, null, null, null).toPageable())
                .isEqualTo(Pageable.unpaged(BookingCriteria.LEGACY_ORDER));

        assertThat(new BookingCriteria(null, null, null, null, null, 0, 10).toPageable())
                .isEqualTo(Pageable.unpaged(BookingCriteria.LEGACY_ORDER));

        assertThat(new BookingCriteria(null, null, null, null, null, 1, 0).toPageable())
                .isEqualTo(Pageable.unpaged(BookingCriteria.LEGACY_ORDER));

        assertThat(new BookingCriteria(null, null, null, null, null, -1, 10).toPageable())
                .isEqualTo(Pageable.unpaged(BookingCriteria.LEGACY_ORDER));
    }

    @Test
    @DisplayName("static factory methods construct expected criteria instances")
    void factoryMethods_constructExpectedCriteria() {
        BookingCriteria all = BookingCriteria.all();
        assertThat(all.carPlate()).isNull();
        assertThat(all.park()).isNull();
        assertThat(all.bookingDate()).isNull();
        assertThat(all.cellPhone()).isNull();
        assertThat(all.status()).isNull();
        assertThat(all.pageNum()).isNull();
        assertThat(all.pageSize()).isNull();

        BookingCriteria unpaged = BookingCriteria.unpaged(
                "粤A12345", "黄麻停车场", "2026-08-20", "13800138000", BookingStatus.ACTIVE);
        assertThat(unpaged.carPlate()).isEqualTo("粤A12345");
        assertThat(unpaged.park()).isEqualTo("黄麻停车场");
        assertThat(unpaged.bookingDate()).isEqualTo("2026-08-20");
        assertThat(unpaged.cellPhone()).isEqualTo("13800138000");
        assertThat(unpaged.status()).isEqualTo(BookingStatus.ACTIVE);
        assertThat(unpaged.isPaged()).isFalse();
    }

    @Test
    @DisplayName("toSpecification builds a non-null specification")
    void toSpecification_returnsNonNullSpecification() {
        BookingCriteria criteria = BookingCriteria.unpaged(
                "粤A12345", "黄麻停车场", "2026-08-20", "13800138000", BookingStatus.ACTIVE);

        assertThat(criteria.toSpecification()).isNotNull();
    }
}
