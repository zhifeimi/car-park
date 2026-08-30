package press.mizhifei.parking.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 停车场查询条件 (Park Query Criteria).
 * <p>
 * Unified query criteria object consolidating search filters, pagination bounds,
 * and string normalization across Controller, Service, and Repository seams.
 */
public record ParkCriteria(
        String name,
        Long lotNum,
        String openDateBegin,
        String openDateEnd,
        String openTime,
        String closeTime,
        Integer pageNum,
        Integer pageSize
) {

    public ParkCriteria {
        name = blankToNull(name);
        openDateBegin = blankToNull(openDateBegin);
        openDateEnd = blankToNull(openDateEnd);
        openTime = blankToNull(openTime);
        closeTime = blankToNull(closeTime);
    }

    /**
     * Constructs a {@link Pageable} from the criteria parameters.
     * Returns {@link Pageable#unpaged()} if pagination is omitted or non-positive.
     */
    public Pageable toPageable() {
        return (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0)
                ? PageRequest.of(pageNum - 1, pageSize)
                : Pageable.unpaged();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
