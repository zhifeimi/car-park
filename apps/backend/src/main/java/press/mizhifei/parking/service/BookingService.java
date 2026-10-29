package press.mizhifei.parking.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.BookingStatus;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.domain.Plates;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.repository.BookingRepository;
import press.mizhifei.parking.repository.ParkRepository;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingUpdateRequest;
import press.mizhifei.parking.web.dto.StatisticsInfo;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * 停车预约运维与生命周期管理 (Booking Operations Deep Module).
 * <p>
 * Consolidates the complete booking lifecycle — intake/creation with concurrency locks,
 * state machine cancellation invariants, administrative queries, and deletion — behind
 * a single cohesive domain interface.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkRepository parkRepository;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, ParkRepository parkRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.parkRepository = parkRepository;
        this.clock = clock;
    }

    /**
     * Atomically process a reservation intent under a pessimistic write lock.
     * Enforces date validity, vehicle exclusivity, open-date range, and capacity.
     *
     * <p>Runs at READ COMMITTED: under MySQL's default REPEATABLE READ the
     * transaction's MVCC snapshot is pinned to its first read, so the checks under the
     * park lock would not see bookings a concurrent intake committed while we waited
     * for the lock — check-then-act would overbook. READ COMMITTED gives every check
     * a fresh view of committed data; the park row lock serializes same-park intakes.
     *
     * @param command citizen booking details
     * @return immutable booking confirmation with assigned ID
     * @throws ServiceException if any domain invariant is violated
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingConfirmation intake(BookingCommand command) {
        // One vehicle, one spelling: "AB-123" and "AB 123" must collide in the checks below.
        String carPlate = Plates.canonical(command.carPlate());
        LocalDate today = LocalDate.now(clock);
        if (command.bookingDate().isBefore(today)) {
            throw new ServiceException("预约日期不能早于今天");
        }

        // Fail fast (advisory): the authoritative exclusivity check happens again
        // under the park lock below, where it sees concurrently committed bookings.
        if (bookingRepository.existsByCarPlateAndBookingDateAndStatus(carPlate, command.bookingDate(), BookingStatus.ACTIVE)) {
            throw new ServiceException("该车牌在所选日期已有生效的预约记录");
        }

        // Target park existence & acquisition of PESSIMISTIC_WRITE lock — from here on,
        // intakes for this park are serialized.
        Park park = parkRepository.findByName(command.park())
                .orElseThrow(() -> new ServiceException("停车场不存在"));

        // Vehicle exclusivity, authoritative: re-checked under the lock with a fresh
        // committed view (see isolation note above).
        if (bookingRepository.existsByCarPlateAndBookingDateAndStatus(carPlate, command.bookingDate(), BookingStatus.ACTIVE)) {
            throw new ServiceException("该车牌在所选日期已有生效的预约记录");
        }

        // Date within park's configured availability window
        if (park.getOpenDateBegin() != null && command.bookingDate().isBefore(park.getOpenDateBegin())) {
            throw new ServiceException("所选日期不在停车场开放预约范围内");
        }
        if (park.getOpenDateEnd() != null && command.bookingDate().isAfter(park.getOpenDateEnd())) {
            throw new ServiceException("所选日期不在停车场开放预约范围内");
        }

        // Capacity check under the park lock, fresh committed view
        long activeCount = bookingRepository.countByParkAndBookingDateAndStatus(command.park(), command.bookingDate(), BookingStatus.ACTIVE);
        if (park.getLotNum() != null && activeCount >= park.getLotNum()) {
            throw new ServiceException("该停车场当日预约名额已满");
        }

        // Create active booking (status = ACTIVE) and persist
        ParkingBooking booking = new ParkingBooking();
        booking.setCarPlate(carPlate);
        booking.setPark(command.park());
        booking.setBookingDate(command.bookingDate());
        booking.setCellPhone(command.cellPhone());
        booking.setRemark(command.remark());
        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        return new BookingConfirmation(
                booking.getId(),
                booking.getCarPlate(),
                booking.getPark(),
                booking.getBookingDate(),
                booking.getCellPhone()
        );
    }

    /**
     * Cancel an active booking and transition state from ACTIVE to CANCELLED.
     * Enforces that the booking must currently be active and for a non-past date.
     *
     * <p>The caller must name the booking's phone: ids are sequential, so an id alone would let
     * anyone cancel everyone's bookings. A wrong phone answers exactly like a missing id, so the
     * endpoint is no existence oracle.
     *
     * @param id booking ID
     * @param cellPhone the phone the booking was made with
     * @throws ServiceException if the booking is missing or not that phone's, already cancelled,
     *                          or for a past date
     */
    @Transactional
    public void cancel(Long id, String cellPhone) {
        if (id == null) {
            throw new ServiceException("预约记录不存在");
        }
        ParkingBooking booking = bookingRepository.findById(id)
                .filter(b -> cellPhone != null && Objects.equals(b.getCellPhone(), cellPhone))
                .orElseThrow(() -> new ServiceException("预约记录不存在"));

        booking.cancel(LocalDate.now(clock));
        bookingRepository.save(booking);
    }

    /**
     * Handles public partial updates. Anonymous callers are restricted strictly
     * to cancellation ({@code status: -1}, proven by the booking's {@code cellPhone}).
     * Arbitrary field mutations are rejected.
     */
    @Transactional
    public void update(BookingUpdateRequest request) {
        if (request.status() == BookingStatus.CANCELLED) {
            cancel(request.id(), request.cellPhone());
            return;
        }
        throw new ServiceException("不支持修改预约记录内容");
    }

    /**
     * Queries bookings matching criteria. Returns paged rows when {@code pageNum}/{@code pageSize}
     * are present, or all rows unpaged.
     */
    public TableData list(BookingCriteria criteria) {
        if (criteria.isPaged()) {
            Page<ParkingBooking> page = bookingRepository.findAll(
                    criteria.toSpecification(),
                    criteria.toPageable());
            return new TableData(page.getTotalElements(), page.getContent());
        }
        List<ParkingBooking> rows = bookingRepository.findAll(
                criteria.toSpecification(),
                BookingCriteria.LEGACY_ORDER);
        return new TableData(rows.size(), rows);
    }

    /**
     * Unpaged query matching criteria (used primarily by the tabular report export stream).
     */
    public List<ParkingBooking> listAll(BookingCriteria criteria) {
        return bookingRepository.findAll(criteria.toSpecification(), BookingCriteria.LEGACY_ORDER);
    }

    /**
     * Latest booking (by create time) for a phone <em>and</em> plate. Both are required: by phone
     * alone, walking a number range would return every citizen's plate, park and booking id.
     * The plate is matched in its canonical spelling ({@link Plates}), as intake stores it.
     */
    public ParkingBooking lastBooking(String cellPhone, String carPlate) {
        return bookingRepository.findFirstByCellPhoneAndCarPlateOrderByCreateTimeDesc(cellPhone, Plates.canonical(carPlate))
                .orElse(null);
    }

    @Transactional
    public void deleteByIds(String ids) {
        List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        bookingRepository.deleteAllByIdInBatch(idList);
    }

    /**
     * Dashboard aggregate. {@code date} stays a String (the dashboard sends
     * {@code yyyy-MM-dd HH:mm:ss}); the SQL parses it with {@code date_format}.
     */
    public StatisticsInfo statistics(String date) {
        return new StatisticsInfo(
                bookingRepository.todayBooking(date),
                bookingRepository.weekBooking(date),
                bookingRepository.monthBooking(date),
                bookingRepository.yearBooking(date));
    }
}
