package press.mizhifei.parking.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.repository.ParkRepository;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkCreateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkUpdateRequest;
import press.mizhifei.parking.web.dto.ParkListRow;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * 停车场管理与运营时段规则 (Park Management & Schedule Invariants Deep Module).
 * <p>
 * Consolidates parking lot capacity definitions, operating schedule invariants,
 * name collision guards, and unified criteria queries behind a single domain interface.
 * Availability is never stored — {@code leftLotNum} is computed dynamically in SQL.
 */
@Service
public class ParkService {

    private final ParkRepository parkRepository;
    private final Clock clock;

    public ParkService(ParkRepository parkRepository, Clock clock) {
        this.parkRepository = parkRepository;
        this.clock = clock;
    }

    /**
     * Queries parks matching criteria. Returns paged rows when {@code pageNum}/{@code pageSize}
     * are present, or all rows unpaged for anonymous citizen H5 invocations.
     */
    public TableData list(ParkCriteria criteria) {
        LocalDate today = LocalDate.now(clock);
        Page<ParkListRow> page = parkRepository.search(
                criteria.name(), criteria.lotNum(),
                criteria.openDateBegin(), criteria.openDateEnd(),
                criteria.openTime(), criteria.closeTime(), today,
                criteria.toPageable());
        // today = the day leftLotNum was computed for; the H5 defaults and bounds its date by it
        return new TableData(page.getTotalElements(), page.getContent()).withToday(today);
    }

    /**
     * Legacy parameter overload delegating to {@link #list(ParkCriteria)}.
     */
    public TableData list(String name, Long lotNum, String openDateBegin, String openDateEnd,
                          String openTime, String closeTime, Integer pageNum, Integer pageSize) {
        return list(new ParkCriteria(name, lotNum, openDateBegin, openDateEnd, openTime, closeTime, pageNum, pageSize));
    }

    /**
     * Unpaged query matching criteria (used primarily by the tabular report export stream).
     */
    public List<ParkListRow> listAll(ParkCriteria criteria) {
        return parkRepository.search(
                criteria.name(), criteria.lotNum(),
                criteria.openDateBegin(), criteria.openDateEnd(),
                criteria.openTime(), criteria.closeTime(), LocalDate.now(clock),
                org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    /**
     * Legacy parameter overload delegating to {@link #listAll(ParkCriteria)}.
     */
    public List<ParkListRow> listAll(String name, Long lotNum, String openDateBegin,
                                     String openDateEnd, String openTime, String closeTime) {
        return listAll(new ParkCriteria(name, lotNum, openDateBegin, openDateEnd, openTime, closeTime, null, null));
    }

    public Park get(Long id) {
        return parkRepository.findById(id).orElse(null);
    }

    /** Remaining spaces for a park on a date; null park → null. */
    public Integer leftLotNum(String park, String date) {
        return parkRepository.leftLotNum(park, date);
    }

    /**
     * Creates a new parking lot entity with validated schedule and unique name invariants.
     */
    @Transactional
    public void create(ParkCreateRequest request) {
        String trimmedName = request.name() == null ? null : request.name().trim();
        if (trimmedName != null && parkRepository.existsByName(trimmedName)) {
            throw new ServiceException("停车场名称已存在");
        }

        Park park = Park.create(
                request.name(),
                request.lotNum(),
                request.openDateBegin(),
                request.openDateEnd(),
                request.openTime(),
                request.closeTime(),
                request.remark()
        );
        parkRepository.save(park);
    }

    /**
     * Partially updates a parking lot, guarding against name collisions and validating
     * combined operational schedule invariants.
     */
    @Transactional
    public void update(ParkUpdateRequest request) {
        Park park = parkRepository.findById(request.id())
                .orElseThrow(() -> new ServiceException("停车场不存在"));

        if (request.name() != null && !request.name().isBlank()) {
            String trimmedName = request.name().trim();
            if (parkRepository.existsByNameAndIdNot(trimmedName, request.id())) {
                throw new ServiceException("停车场名称已存在");
            }
        }

        park.applyPartialUpdate(
                request.name(),
                request.lotNum(),
                request.openDateBegin(),
                request.openDateEnd(),
                request.openTime(),
                request.closeTime(),
                request.remark()
        );
        parkRepository.save(park);
    }

    /** Comma-separated ids, legacy {@code DELETE /parking/park/{ids}} style. */
    @Transactional
    public void deleteByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        parkRepository.deleteAllByIdInBatch(idList);
    }
}
