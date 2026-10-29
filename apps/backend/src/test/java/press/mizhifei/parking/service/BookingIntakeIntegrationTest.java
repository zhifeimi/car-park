package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import press.mizhifei.parking.DbIntegrationTest;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.BookingStatus;
import press.mizhifei.parking.web.dto.ParkListRow;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingUpdateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkCreateRequest;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * Booking Intake &amp; Operations, tested through its interface against the real local
 * MySQL (the adapter the lock, the capacity arithmetic, and the exclusivity invariant
 * actually run against in production). Replaces the former mock-based BookingServiceTest:
 * mocks verified that methods were called, never that the invariants held.
 *
 * <p>Boot 4 dropped the {@code @DataJpaTest} slice, so the full application context is
 * the integration-test shape; tests still run without a wrapping transaction (see
 * {@link DbIntegrationTest}).
 */
@SpringBootTest
class BookingIntakeIntegrationTest extends DbIntegrationTest {

    private static final String CAR_PLATE = "粤A12345";
    private static final String OTHER_PLATE = "粤B88888";
    private static final String PARK_NAME = "黄麻停车场";
    private static final String SECOND_PARK = "岭头停车场";
    private static final String CELL_PHONE = "13800138000";
    @Autowired
    private Clock parkClock;

    /** The park's day — the service's clock, not the test JVM's default zone. */
    private LocalDate today;
    private LocalDate tomorrow;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ParkService parkService;

    @BeforeEach
    void parkDay() {
        today = LocalDate.now(parkClock);
        tomorrow = today.plusDays(1);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private void createPark(String name, long lotNum) {
        createPark(name, lotNum, today, today.plusMonths(3));
    }

    private void createPark(String name, long lotNum, LocalDate begin, LocalDate end) {
        parkService.create(new ParkCreateRequest(name, lotNum, begin, end, null, null, null));
    }

    private BookingCommand cmd(String carPlate, String park, LocalDate date) {
        return new BookingCommand(carPlate, park, date, CELL_PHONE, "集成测试");
    }

    private int activeBookingCount() {
        Integer count = jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM cp_parking_booking WHERE status = 0", Integer.class);
        return count == null ? -1 : count;
    }

    /**
     * Fires all attempts at the same instant and returns one boolean per attempt
     * (true = the call succeeded). Any other exception than {@code expectedFailure}
     * fails the test via {@link Future#get()}.
     */
    private List<Future<Boolean>> race(List<Callable<Boolean>> attempts,
                                       Class<? extends Exception> expectedFailure) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> attempt : attempts) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    return attempt.call();
                } catch (Exception e) {
                    if (expectedFailure.isInstance(e)) {
                        return false;
                    }
                    throw e;
                }
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).as("race threads finish").isTrue();
        return futures;
    }

    private List<Future<Boolean>> race(int threads, Callable<Boolean> attempt,
                                       Class<? extends Exception> expectedFailure) throws Exception {
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            attempts.add(attempt);
        }
        return race(attempts, expectedFailure);
    }

    private static long successes(List<Future<Boolean>> futures) throws Exception {
        long count = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                count++;
            }
        }
        return count;
    }

    // ─── intake invariants (sequential) ─────────────────────────────────────

    @Test
    @DisplayName("valid command creates one ACTIVE booking and returns its confirmation")
    void intake_validCommand_createsActiveBooking() {
        createPark(PARK_NAME, 2L);

        BookingConfirmation confirmation = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        assertThat(confirmation.id()).isNotNull();
        assertThat(confirmation.carPlate()).isEqualTo(CAR_PLATE);
        assertThat(confirmation.park()).isEqualTo(PARK_NAME);
        assertThat(confirmation.bookingDate()).isEqualTo(tomorrow);
        assertThat(confirmation.cellPhone()).isEqualTo(CELL_PHONE);
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    @Test
    void intake_pastDate_rejected() {
        createPark(PARK_NAME, 2L);

        assertThatThrownBy(() -> bookingService.intake(cmd(CAR_PLATE, PARK_NAME, today.minusDays(1))))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约日期不能早于今天");
        assertThat(activeBookingCount()).isZero();
    }

    @Test
    void intake_duplicatePlateSameDate_rejectedSequentially() {
        createPark(PARK_NAME, 2L);
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        assertThatThrownBy(() -> bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该车牌在所选日期已有生效的预约记录");
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("one vehicle written with different separators is still one vehicle")
    void intake_samePlateDifferentSeparators_rejected() {
        createPark(PARK_NAME, 5L);
        BookingConfirmation first = bookingService.intake(cmd("ab-123", PARK_NAME, tomorrow));
        assertThat(first.carPlate()).isEqualTo("AB123");

        for (String spelling : new String[] {"AB 123", "AB123", "AB·123"}) {
            assertThatThrownBy(() -> bookingService.intake(cmd(spelling, PARK_NAME, tomorrow)))
                    .as(spelling)
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("该车牌在所选日期已有生效的预约记录");
        }
        assertThat(activeBookingCount()).isEqualTo(1);
        assertThat(bookingService.lastBooking(CELL_PHONE, "AB 123").getId()).isEqualTo(first.id());
    }

    @Test
    void intake_unknownPark_rejected() {
        assertThatThrownBy(() -> bookingService.intake(cmd(CAR_PLATE, "不存在的停车场", tomorrow)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场不存在");
        assertThat(activeBookingCount()).isZero();
    }

    @Test
    void intake_dateBeforeOpenWindow_rejected() {
        createPark(PARK_NAME, 2L, tomorrow.plusDays(5), tomorrow.plusMonths(2));

        assertThatThrownBy(() -> bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("所选日期不在停车场开放预约范围内");
    }

    @Test
    void intake_dateAfterOpenWindow_rejected() {
        createPark(PARK_NAME, 2L, today.minusDays(10), today);

        assertThatThrownBy(() -> bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("所选日期不在停车场开放预约范围内");
    }

    @Test
    void intake_capacityFull_rejectedSequentially() {
        createPark(PARK_NAME, 1L);
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        assertThatThrownBy(() -> bookingService.intake(cmd(OTHER_PLATE, PARK_NAME, tomorrow)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该停车场当日预约名额已满");
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    // ─── concurrency: capacity ──────────────────────────────────────────────

    @Test
    @DisplayName("capacity race: the last slot has exactly one winner")
    void capacityRace_lastSlotExactlyOneWinner() throws Exception {
        createPark(PARK_NAME, 1L);

        List<Future<Boolean>> futures = race(2,
                () -> {
                    bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));
                    return true;
                },
                ServiceException.class);

        assertThat(successes(futures)).as("exactly one of two citizens wins the last slot").isEqualTo(1);
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    // ─── concurrency: vehicle exclusivity ───────────────────────────────────

    @Test
    @DisplayName("exclusivity race (one park): at most one ACTIVE booking per plate per date")
    void exclusivityRace_samePark_atMostOneWinner() throws Exception {
        createPark(PARK_NAME, 2L);

        List<Future<Boolean>> futures = race(2,
                () -> {
                    bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));
                    return true;
                },
                ServiceException.class);

        assertThat(successes(futures)).as("at most one of the two racers may win").isLessThanOrEqualTo(1);
        Integer active = jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM cp_parking_booking WHERE car_plate = ? AND booking_date = ? AND status = 0",
                Integer.class, CAR_PLATE, tomorrow);
        assertThat(active).as("at most one ACTIVE booking per plate per date").isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("exclusivity race (across parks): locks on different park rows cannot both let the plate through")
    void exclusivityRace_acrossParks_atMostOneWinner() throws Exception {
        createPark(PARK_NAME, 2L);
        createPark(SECOND_PARK, 2L);

        List<Future<Boolean>> futures = race(List.of(
                (Callable<Boolean>) () -> {
                    bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));
                    return true;
                },
                () -> {
                    bookingService.intake(cmd(CAR_PLATE, SECOND_PARK, tomorrow));
                    return true;
                }), ServiceException.class);

        assertThat(successes(futures)).as("at most one of the two racers may win").isLessThanOrEqualTo(1);
        Integer active = jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM cp_parking_booking WHERE car_plate = ? AND booking_date = ? AND status = 0",
                Integer.class, CAR_PLATE, tomorrow);
        assertThat(active).as("at most one ACTIVE booking per plate per date across all parks").isLessThanOrEqualTo(1);
    }

    // ─── cancellation & lifecycle ───────────────────────────────────────────

    @Test
    void cancel_activeBooking_transitionsReleasesSlotAndPlate() {
        createPark(PARK_NAME, 2L);
        BookingConfirmation confirmation = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        bookingService.cancel(confirmation.id(), CELL_PHONE);

        Integer cancelled = jdbcTemplate().queryForObject(
                "SELECT status FROM cp_parking_booking WHERE id = ?", Integer.class, confirmation.id());
        assertThat(cancelled).isEqualTo(BookingStatus.CANCELLED.getCode());
        assertThat(activeBookingCount()).isZero();
        // the released plate can book again
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    @Test
    void cancel_wrongOrMissingPhone_rejectedLikeMissingId() {
        createPark(PARK_NAME, 2L);
        BookingConfirmation confirmation = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        assertThatThrownBy(() -> bookingService.cancel(confirmation.id(), "13900139000"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约记录不存在");
        assertThatThrownBy(() -> bookingService.update(new BookingUpdateRequest(
                confirmation.id(), null, null, null, null, null, BookingStatus.CANCELLED)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约记录不存在");
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    @Test
    void cancel_missingId_rejected() {
        assertThatThrownBy(() -> bookingService.cancel(99_999L, CELL_PHONE))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约记录不存在");
    }

    @Test
    void cancel_alreadyCancelled_rejected() {
        createPark(PARK_NAME, 2L);
        BookingConfirmation confirmation = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));
        bookingService.cancel(confirmation.id(), CELL_PHONE);

        assertThatThrownBy(() -> bookingService.cancel(confirmation.id(), CELL_PHONE))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该预约记录已取消或已失效");
    }

    @Test
    void cancel_pastDateBooking_rejected() {
        createPark(PARK_NAME, 2L);
        jdbcTemplate().update(
                "INSERT INTO cp_parking_booking (car_plate, park, booking_date, cell_phone, status, create_by, create_time)"
                        + " VALUES (?, ?, ?, ?, ?, 'system', NOW())",
                CAR_PLATE, PARK_NAME, today.minusDays(1), CELL_PHONE, BookingStatus.ACTIVE.getCode());
        Long id = jdbcTemplate().queryForObject(
                "SELECT id FROM cp_parking_booking WHERE car_plate = ?", Long.class, CAR_PLATE);

        assertThatThrownBy(() -> bookingService.cancel(id, CELL_PHONE))
                .isInstanceOf(ServiceException.class)
                .hasMessage("无法取消已过期的历史预约");
    }

    @Test
    void update_cancelRequest_cancels() {
        createPark(PARK_NAME, 2L);
        BookingConfirmation confirmation = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        bookingService.update(new BookingUpdateRequest(
                confirmation.id(), null, null, null, CELL_PHONE, null, BookingStatus.CANCELLED));

        assertThat(activeBookingCount()).isZero();
    }

    @Test
    void update_arbitraryMutation_rejected() {
        createPark(PARK_NAME, 2L);
        BookingConfirmation confirmation = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));

        assertThatThrownBy(() -> bookingService.update(new BookingUpdateRequest(
                confirmation.id(), OTHER_PLATE, null, null, null, null, BookingStatus.ACTIVE)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("不支持修改预约记录内容");
        assertThat(activeBookingCount()).isEqualTo(1);
    }

    // ─── queries & admin operations ─────────────────────────────────────────

    @Test
    void lastBooking_returnsMostRecentForPhoneAndPlate() throws InterruptedException {
        createPark(PARK_NAME, 5L);
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, today));
        // create_time has second precision — separate the rows so ordering is deterministic
        Thread.sleep(1100);
        bookingService.intake(cmd(OTHER_PLATE, PARK_NAME, tomorrow));

        var last = bookingService.lastBooking(CELL_PHONE, OTHER_PLATE);

        assertThat(last).isNotNull();
        assertThat(last.getBookingDate()).isEqualTo(tomorrow);
        assertThat(bookingService.lastBooking(CELL_PHONE, CAR_PLATE).getBookingDate()).isEqualTo(today);
        // phone alone is not enough — the plate must match too
        assertThat(bookingService.lastBooking(CELL_PHONE, "粤C00000")).isNull();
        assertThat(bookingService.lastBooking("13900139000", OTHER_PLATE)).isNull();
    }

    @Test
    void deleteByIds_removesBatch() {
        createPark(PARK_NAME, 5L);
        long first = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, today)).id();
        long second = bookingService.intake(cmd(OTHER_PLATE, PARK_NAME, today)).id();

        bookingService.deleteByIds(first + ", " + second);

        assertThat(activeBookingCount()).isZero();
    }

    @Test
    @DisplayName("list with BookingCriteria filters by fields and handles pagination")
    void list_withCriteria_filtersAndPages() {
        createPark(PARK_NAME, 5L);
        createPark(SECOND_PARK, 5L);
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, today));
        bookingService.intake(cmd(OTHER_PLATE, SECOND_PARK, today));

        // Filter by park
        TableData parkFilter = bookingService.list(BookingCriteria.unpaged(null, PARK_NAME, null, null, null));
        assertThat(parkFilter.getTotal()).isEqualTo(1);

        // Filter by plate
        TableData plateFilter = bookingService.list(BookingCriteria.unpaged(CAR_PLATE, null, null, null, null));
        assertThat(plateFilter.getTotal()).isEqualTo(1);

        // Paged query
        BookingCriteria pagedCriteria = new BookingCriteria(null, null, null, null, null, 1, 1);
        TableData pagedData = bookingService.list(pagedCriteria);
        assertThat(pagedData.getTotal()).isEqualTo(2);
        assertThat(pagedData.getRows()).hasSize(1);
    }

    @Test
    @DisplayName("listAll with BookingCriteria returns unpaged rows")
    void listAll_withCriteria_returnsMatching() {
        createPark(PARK_NAME, 5L);
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, today));
        bookingService.intake(cmd(OTHER_PLATE, PARK_NAME, today));

        var all = bookingService.listAll(BookingCriteria.all());
        assertThat(all).hasSize(2);

        var filtered = bookingService.listAll(BookingCriteria.unpaged(CAR_PLATE, null, null, null, null));
        assertThat(filtered).hasSize(1);
    }

    // ─── availability (computed, never stored) ──────────────────────────────

    @Test
    @DisplayName("leftLotNum reflects intake and frees on cancel")
    void leftLotNum_followsIntakeAndCancellation() {
        createPark(PARK_NAME, 3L);
        BookingConfirmation first = bookingService.intake(cmd(CAR_PLATE, PARK_NAME, tomorrow));
        bookingService.intake(cmd(OTHER_PLATE, PARK_NAME, tomorrow));

        assertThat(parkService.leftLotNum(PARK_NAME, tomorrow.toString())).isEqualTo(1);

        bookingService.cancel(first.id(), CELL_PHONE);

        assertThat(parkService.leftLotNum(PARK_NAME, tomorrow.toString())).isEqualTo(2);
    }

    @Test
    @DisplayName("park list computes today's leftLotNum from real intake rows")
    void parkList_computesTodaysLeftLotNum() {
        createPark(PARK_NAME, 5L);
        bookingService.intake(cmd(CAR_PLATE, PARK_NAME, today));
        bookingService.intake(cmd(OTHER_PLATE, PARK_NAME, today));

        TableData data = parkService.list(new ParkCriteria(null, null, null, null, null, null, null, null));

        assertThat(data.getTotal()).isEqualTo(1);
        ParkListRow row = (ParkListRow) data.getRows().get(0);
        assertThat(row.getLeftLotNum()).isEqualTo(3L);
    }
}
