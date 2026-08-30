package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link ParkCriteria} query object:
 * blank-to-null string normalization and pageable translation.
 */
class ParkCriteriaTest {

    @Test
    void compactConstructor_normalizesBlankStringsToNullAndTrims() {
        ParkCriteria criteria = new ParkCriteria(
                "  黄麻  ",
                10L,
                "  2026-09-01  ",
                "   ",
                "",
                null,
                1,
                20
        );

        assertThat(criteria.name()).isEqualTo("黄麻");
        assertThat(criteria.lotNum()).isEqualTo(10L);
        assertThat(criteria.openDateBegin()).isEqualTo("2026-09-01");
        assertThat(criteria.openDateEnd()).isNull();
        assertThat(criteria.openTime()).isNull();
        assertThat(criteria.closeTime()).isNull();
    }

    @Test
    void toPageable_validPagination_returnsPageRequest() {
        ParkCriteria criteria = new ParkCriteria(null, null, null, null, null, null, 2, 15);

        Pageable pageable = criteria.toPageable();

        assertThat(pageable.isPaged()).isTrue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable).isEqualTo(PageRequest.of(1, 15));
    }

    @Test
    void toPageable_nullOrNonPositivePagination_returnsUnpaged() {
        assertThat(new ParkCriteria(null, null, null, null, null, null, null, null).toPageable())
                .isEqualTo(Pageable.unpaged());

        assertThat(new ParkCriteria(null, null, null, null, null, null, 0, 10).toPageable())
                .isEqualTo(Pageable.unpaged());

        assertThat(new ParkCriteria(null, null, null, null, null, null, 1, 0).toPageable())
                .isEqualTo(Pageable.unpaged());

        assertThat(new ParkCriteria(null, null, null, null, null, null, -1, 10).toPageable())
                .isEqualTo(Pageable.unpaged());
    }
}
