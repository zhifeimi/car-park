package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.repository.ParkRepository;
import press.mizhifei.parking.web.dto.ParkListRow;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkCreateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkUpdateRequest;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * Unit tests for {@link ParkService}:
 * name collision guards, schedule invariant delegation, criteria searches, and batch operations.
 */
@ExtendWith(MockitoExtension.class)
class ParkServiceTest {

    @Mock
    ParkRepository parkRepository;

    @InjectMocks
    ParkService parkService;

    private static final String PARK_NAME = "黄麻社区停车场";
    private static final LocalDate DATE_BEGIN = LocalDate.of(2026, 9, 1);
    private static final LocalDate DATE_END = LocalDate.of(2026, 12, 31);
    private static final LocalTime TIME_OPEN = LocalTime.of(8, 0, 0);
    private static final LocalTime TIME_CLOSE = LocalTime.of(22, 0, 0);

    // ─── Create Invariants & Collision Guards ─────────────────────────────────

    @Test
    void create_validRequest_savesPark() {
        when(parkRepository.existsByName(PARK_NAME)).thenReturn(false);

        ParkCreateRequest request = new ParkCreateRequest(
                PARK_NAME,
                30L,
                DATE_BEGIN,
                DATE_END,
                TIME_OPEN,
                TIME_CLOSE,
                "测试备注"
        );

        parkService.create(request);

        verify(parkRepository).existsByName(PARK_NAME);
        verify(parkRepository).save(any(Park.class));
    }

    @Test
    void create_duplicateName_throwsServiceException() {
        when(parkRepository.existsByName(PARK_NAME)).thenReturn(true);

        ParkCreateRequest request = new ParkCreateRequest(
                PARK_NAME,
                30L,
                DATE_BEGIN,
                DATE_END,
                TIME_OPEN,
                TIME_CLOSE,
                null
        );

        assertThatThrownBy(() -> parkService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场名称已存在");

        verify(parkRepository, never()).save(any());
    }

    @Test
    void create_invalidSchedule_throwsServiceExceptionFromDomainValidation() {
        when(parkRepository.existsByName(PARK_NAME)).thenReturn(false);

        ParkCreateRequest request = new ParkCreateRequest(
                PARK_NAME,
                30L,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 9, 1),
                TIME_OPEN,
                TIME_CLOSE,
                null
        );

        assertThatThrownBy(() -> parkService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("可预约起始日期不能晚于结束日期");

        verify(parkRepository, never()).save(any());
    }

    // ─── Update Invariants & Collision Guards ─────────────────────────────────

    @Test
    void update_validRequest_mergesAndSavesPark() {
        Park existing = Park.create(PARK_NAME, 20L, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, "旧备注");
        existing.setId(1L);
        when(parkRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(parkRepository.existsByNameAndIdNot("新车场名称", 1L)).thenReturn(false);

        ParkUpdateRequest request = new ParkUpdateRequest(
                1L,
                "新车场名称",
                50L,
                null,
                null,
                null,
                null,
                "新备注"
        );

        parkService.update(request);

        assertThat(existing.getName()).isEqualTo("新车场名称");
        assertThat(existing.getLotNum()).isEqualTo(50L);
        assertThat(existing.getRemark()).isEqualTo("新备注");
        verify(parkRepository).save(existing);
    }

    @Test
    void update_parkNotFound_throwsServiceException() {
        when(parkRepository.findById(99L)).thenReturn(Optional.empty());

        ParkUpdateRequest request = new ParkUpdateRequest(99L, "新名称", null, null, null, null, null, null);

        assertThatThrownBy(() -> parkService.update(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场不存在");

        verify(parkRepository, never()).save(any());
    }

    @Test
    void update_nameCollisionOnOtherPark_throwsServiceException() {
        Park existing = Park.create(PARK_NAME, 20L, DATE_BEGIN, DATE_END, TIME_OPEN, TIME_CLOSE, null);
        existing.setId(1L);
        when(parkRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(parkRepository.existsByNameAndIdNot("已被占用的名称", 1L)).thenReturn(true);

        ParkUpdateRequest request = new ParkUpdateRequest(1L, "已被占用的名称", null, null, null, null, null, null);

        assertThatThrownBy(() -> parkService.update(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场名称已存在");

        verify(parkRepository, never()).save(any());
    }

    @Test
    void update_violatingCombinedSchedule_throwsServiceException() {
        Park existing = Park.create(PARK_NAME, 20L, DATE_BEGIN, LocalDate.of(2026, 9, 30), TIME_OPEN, TIME_CLOSE, null);
        existing.setId(1L);
        when(parkRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Attempting to move start date to after the existing end date
        ParkUpdateRequest request = new ParkUpdateRequest(
                1L,
                null,
                null,
                LocalDate.of(2026, 10, 15),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> parkService.update(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("可预约起始日期不能晚于结束日期");

        verify(parkRepository, never()).save(any());
    }

    // ─── Query Criteria Delegation ───────────────────────────────────────────

    @Test
    void list_withCriteria_delegatesToRepositoryWithPageable() {
        ParkCriteria criteria = new ParkCriteria("黄麻", 10L, "2026-09-01", null, null, null, 1, 10);
        when(parkRepository.search(
                eq("黄麻"), eq(10L), eq("2026-09-01"), eq(null), eq(null), eq(null), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        TableData data = parkService.list(criteria);

        assertThat(data).isNotNull();
        assertThat(data.getTotal()).isEqualTo(0);
        verify(parkRepository).search(
                eq("黄麻"), eq(10L), eq("2026-09-01"), eq(null), eq(null), eq(null), eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void listAll_withCriteria_delegatesToRepositoryUnpaged() {
        ParkCriteria criteria = new ParkCriteria("黄麻", null, null, null, null, null, null, null);
        when(parkRepository.search(
                eq("黄麻"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(Pageable.unpaged())
        )).thenReturn(new PageImpl<>(List.of()));

        List<ParkListRow> rows = parkService.listAll(criteria);

        assertThat(rows).isEmpty();
        verify(parkRepository).search(
                eq("黄麻"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(Pageable.unpaged())
        );
    }

    // ─── Queries & Delete Operations ─────────────────────────────────────────

    @Test
    void deleteByIds_commaSeparated_deletesBatch() {
        parkService.deleteByIds("10, 20, 30");

        verify(parkRepository).deleteAllByIdInBatch(List.of(10L, 20L, 30L));
    }

    @Test
    void get_returnsEntity() {
        Park park = new Park();
        park.setId(5L);
        when(parkRepository.findById(5L)).thenReturn(Optional.of(park));

        Park result = parkService.get(5L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void leftLotNum_delegatesToRepository() {
        when(parkRepository.leftLotNum("黄麻", "2026-09-01")).thenReturn(15);

        Integer count = parkService.leftLotNum("黄麻", "2026-09-01");

        assertThat(count).isEqualTo(15);
    }
}
