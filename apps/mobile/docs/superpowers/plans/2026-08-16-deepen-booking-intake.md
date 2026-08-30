# Deepen Booking Intake Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deepen `BookingService.create()` to atomically validate availability with a pessimistic lock, reject past dates, reject vehicle booking overlaps across lots, and return the generated booking ID so the mobile success page has a stable reference.

**Architecture:** 
1. `ParkRepository` acquires a `PESSIMISTIC_WRITE` lock on the park row during booking creation to serialize concurrent bookings for the same lot.
2. `BookingService.create()` enforces domain invariants (past dates, vehicle date overlap across all parks, park existence, park open-date window, and lot capacity) inside a single `@Transactional` boundary and returns `Long` (booking ID).
3. `BookingController.add()` returns `R<Long>` and `SecurityConfig` permits anonymous `GET /parking/booking/{id}`.
4. Mobile frontend passes the returned booking ID via route query `#/success?id=...` and fetches directly by ID with fallback to phone lookup.

**Tech Stack:** Java 17+ / Spring Boot 4.1 / Spring Data JPA / Hibernate 7 / Mockito / Vue 3 / Vant 4 / Deno + Vite

**Spec:** Grilling design agreement recorded in conversation (2026-08-16).

## Global Constraints

- **Zero DB Schema Changes**: Existing `cp_park` and `cp_parking_booking` schemas are frozen; no new tables or columns.
- **Java environment**: Use `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home` when running `./mvnw`.
- **Frontend Deno environment**: No `node_modules`; run tasks via `cd car-park-mobile && deno task build && deno lint`.
- **Error messaging**: All business validation failures throw `press.mizhifei.parking.common.ServiceException` with exact Chinese messages.

---

### Task 1: Repository Queries for Concurrency and Invariant Verification

**Files:**
- Modify: `car-park-server/src/main/java/press/mizhifei/parking/repository/ParkRepository.java`
- Modify: `car-park-server/src/main/java/press/mizhifei/parking/repository/BookingRepository.java`

**Interfaces:**
- Produces:
  - `ParkRepository.findByName(String name)`: `Optional<Park>` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`
  - `BookingRepository.existsByCarPlateAndBookingDateAndStatus(String carPlate, LocalDate bookingDate, Integer status)`: `boolean`
  - `BookingRepository.countByParkAndBookingDateAndStatus(String park, LocalDate bookingDate, Integer status)`: `long`

- [ ] **Step 1: Update `ParkRepository.java`**

Add pessimistic write lock query by park name:

```java
package press.mizhifei.parking.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.web.dto.ParkListRow;

@Repository
public interface ParkRepository extends JpaRepository<Park, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Park> findByName(String name);

    // existing queries (search, leftLotNum, searchAll)...
```

- [ ] **Step 2: Update `BookingRepository.java`**

Add vehicle overlap check and active booking count queries:

```java
package press.mizhifei.parking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.web.dto.ParkBookingStat;

@Repository
public interface BookingRepository extends JpaRepository<ParkingBooking, Long>,
        JpaSpecificationExecutor<ParkingBooking> {

    Optional<ParkingBooking> findFirstByCellPhoneOrderByCreateTimeDesc(String cellPhone);

    boolean existsByCarPlateAndBookingDateAndStatus(String carPlate, LocalDate bookingDate, Integer status);

    long countByParkAndBookingDateAndStatus(String park, LocalDate bookingDate, Integer status);

    // existing queries (todayBooking, weekBooking, monthBooking, yearBooking)...
```

- [ ] **Step 3: Verify compilation**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home && cd $HOME/projects/car-park/car-park-server && ./mvnw test-compile`
Expected: BUILD SUCCESS

---

### Task 2: Deepen `BookingService.create()` with TDD Unit Tests

**Files:**
- Modify: `car-park-server/src/main/java/press/mizhifei/parking/service/BookingService.java`
- Create: `car-park-server/src/test/java/press/mizhifei/parking/service/BookingServiceTest.java`

**Interfaces:**
- Consumes: `ParkRepository`, `BookingRepository`
- Produces: `BookingService.create(BookingCreateRequest request)` returning `Long`

- [ ] **Step 1: Write unit tests in `BookingServiceTest.java`**

```java
package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.repository.BookingRepository;
import press.mizhifei.parking.repository.ParkRepository;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingCreateRequest;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkRepository parkRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, parkRepository);
    }

    private BookingCreateRequest validRequest(LocalDate date) {
        return new BookingCreateRequest("粤A12345", "黄麻停车场", date, "13800138000", "测试备注");
    }

    private Park createPark(String name, long lotNum, LocalDate begin, LocalDate end) {
        Park park = new Park();
        park.setId(1L);
        park.setName(name);
        park.setLotNum(lotNum);
        park.setOpenDateBegin(begin);
        park.setOpenDateEnd(end);
        return park;
    }

    @Test
    void create_success_returnsBookingId() {
        LocalDate today = LocalDate.now();
        BookingCreateRequest request = validRequest(today);
        Park park = createPark("黄麻停车场", 10L, today.minusDays(1), today.plusDays(10));

        when(bookingRepository.existsByCarPlateAndBookingDateAndStatus("粤A12345", today, 0)).thenReturn(false);
        when(parkRepository.findByName("黄麻停车场")).thenReturn(Optional.of(park));
        when(bookingRepository.countByParkAndBookingDateAndStatus("黄麻停车场", today, 0)).thenReturn(3L);
        when(bookingRepository.save(any(ParkingBooking.class))).thenAnswer(invocation -> {
            ParkingBooking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        Long id = bookingService.create(request);

        assertThat(id).isEqualTo(100L);
        verify(bookingRepository).save(any(ParkingBooking.class));
    }

    @Test
    void create_pastDate_throwsServiceException() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        BookingCreateRequest request = validRequest(yesterday);

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("预约日期不能早于今天");
    }

    @Test
    void create_duplicateCarPlateOnSameDate_throwsServiceException() {
        LocalDate today = LocalDate.now();
        BookingCreateRequest request = validRequest(today);

        when(bookingRepository.existsByCarPlateAndBookingDateAndStatus("粤A12345", today, 0)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该车牌在所选日期已有生效的预约记录");
    }

    @Test
    void create_parkNotFound_throwsServiceException() {
        LocalDate today = LocalDate.now();
        BookingCreateRequest request = validRequest(today);

        when(bookingRepository.existsByCarPlateAndBookingDateAndStatus("粤A12345", today, 0)).thenReturn(false);
        when(parkRepository.findByName("黄麻停车场")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("停车场不存在");
    }

    @Test
    void create_dateBeforeOpenDateBegin_throwsServiceException() {
        LocalDate today = LocalDate.now();
        BookingCreateRequest request = validRequest(today);
        Park park = createPark("黄麻停车场", 10L, today.plusDays(1), today.plusDays(10));

        when(bookingRepository.existsByCarPlateAndBookingDateAndStatus("粤A12345", today, 0)).thenReturn(false);
        when(parkRepository.findByName("黄麻停车场")).thenReturn(Optional.of(park));

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("所选日期不在停车场开放预约范围内");
    }

    @Test
    void create_dateAfterOpenDateEnd_throwsServiceException() {
        LocalDate today = LocalDate.now().plusDays(20);
        BookingCreateRequest request = validRequest(today);
        Park park = createPark("黄麻停车场", 10L, today.minusDays(30), today.minusDays(1));

        when(bookingRepository.existsByCarPlateAndBookingDateAndStatus("粤A12345", today, 0)).thenReturn(false);
        when(parkRepository.findByName("黄麻停车场")).thenReturn(Optional.of(park));

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("所选日期不在停车场开放预约范围内");
    }

    @Test
    void create_capacityFull_throwsServiceException() {
        LocalDate today = LocalDate.now();
        BookingCreateRequest request = validRequest(today);
        Park park = createPark("黄麻停车场", 5L, today.minusDays(1), today.plusDays(10));

        when(bookingRepository.existsByCarPlateAndBookingDateAndStatus("粤A12345", today, 0)).thenReturn(false);
        when(parkRepository.findByName("黄麻停车场")).thenReturn(Optional.of(park));
        when(bookingRepository.countByParkAndBookingDateAndStatus("黄麻停车场", today, 0)).thenReturn(5L);

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("该停车场当日预约名额已满");
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home && cd $HOME/projects/car-park/car-park-server && ./mvnw test -Dtest=BookingServiceTest`
Expected: Compilation failure or FAIL (constructor and `create()` signature mismatch).

- [ ] **Step 3: Implement deepened `BookingService.java`**

Update `BookingService.java` with the injected `ParkRepository` and the deepened `create` method:

```java
package press.mizhifei.parking.service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.repository.BookingRepository;
import press.mizhifei.parking.repository.ParkRepository;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingCreateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingUpdateRequest;
import press.mizhifei.parking.web.dto.StatisticsInfo;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * 停车预约业务. List filters and ordering are verbatim from the legacy
 * {@code selectParkingBookingList}; the statistics queries live in {@link BookingRepository}.
 */
@Service
public class BookingService {

    /** Legacy fixed order: {@code booking_date desc, create_time desc, cell_phone, create_by, car_plate}. */
    private static final Sort LEGACY_ORDER = Sort.by(
            Sort.Order.desc("bookingDate"),
            Sort.Order.desc("createTime"),
            Sort.Order.asc("cellPhone"),
            Sort.Order.asc("createBy"),
            Sort.Order.asc("carPlate"));

    private final BookingRepository bookingRepository;
    private final ParkRepository parkRepository;

    public BookingService(BookingRepository bookingRepository, ParkRepository parkRepository) {
        this.bookingRepository = bookingRepository;
        this.parkRepository = parkRepository;
    }

    /** Paged when {@code pageNum}/{@code pageSize} are present, all rows otherwise. */
    public TableData list(String carPlate, String park, String bookingDate, String cellPhone,
                          Integer status, Integer pageNum, Integer pageSize) {
        Specification<ParkingBooking> spec = filter(carPlate, park, bookingDate, cellPhone, status);
        List<ParkingBooking> rows;
        long total;
        if (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0) {
            Page<ParkingBooking> page = bookingRepository.findAll(spec,
                    PageRequest.of(pageNum - 1, pageSize, LEGACY_ORDER));
            rows = page.getContent();
            total = page.getTotalElements();
        } else {
            rows = bookingRepository.findAll(spec, LEGACY_ORDER);
            total = rows.size();
        }
        return new TableData(total, rows);
    }

    public List<ParkingBooking> listAll(String carPlate, String park, String bookingDate,
                                        String cellPhone, Integer status) {
        return bookingRepository.findAll(filter(carPlate, park, bookingDate, cellPhone, status), LEGACY_ORDER);
    }

    private Specification<ParkingBooking> filter(String carPlate, String park, String bookingDate,
                                                 String cellPhone, Integer status) {
        LocalDate date = parseDate(bookingDate);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(carPlate)) {
                predicates.add(cb.equal(root.get("carPlate"), carPlate));
            }
            if (hasText(park)) {
                predicates.add(cb.equal(root.get("park"), park));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("bookingDate"), date));
            }
            if (hasText(cellPhone)) {
                predicates.add(cb.equal(root.get("cellPhone"), cellPhone));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public ParkingBooking lastByCellPhone(String cellPhone) {
        return bookingRepository.findFirstByCellPhoneOrderByCreateTimeDesc(cellPhone).orElse(null);
    }

    public ParkingBooking get(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    /**
     * Public booking creation with full invariant enforcement under a pessimistic write lock.
     * Returns the created booking ID.
     */
    @Transactional
    public Long create(BookingCreateRequest request) {
        LocalDate today = LocalDate.now();
        if (request.bookingDate().isBefore(today)) {
            throw new ServiceException("预约日期不能早于今天");
        }

        if (bookingRepository.existsByCarPlateAndBookingDateAndStatus(request.carPlate(), request.bookingDate(), 0)) {
            throw new ServiceException("该车牌在所选日期已有生效的预约记录");
        }

        Park park = parkRepository.findByName(request.park())
                .orElseThrow(() -> new ServiceException("停车场不存在"));

        if (park.getOpenDateBegin() != null && request.bookingDate().isBefore(park.getOpenDateBegin())) {
            throw new ServiceException("所选日期不在停车场开放预约范围内");
        }
        if (park.getOpenDateEnd() != null && request.bookingDate().isAfter(park.getOpenDateEnd())) {
            throw new ServiceException("所选日期不在停车场开放预约范围内");
        }

        long activeCount = bookingRepository.countByParkAndBookingDateAndStatus(request.park(), request.bookingDate(), 0);
        if (park.getLotNum() != null && activeCount >= park.getLotNum()) {
            throw new ServiceException("该停车场当日预约名额已满");
        }

        ParkingBooking booking = new ParkingBooking();
        booking.setCarPlate(request.carPlate());
        booking.setPark(request.park());
        booking.setBookingDate(request.bookingDate());
        booking.setCellPhone(request.cellPhone());
        booking.setRemark(request.remark());
        booking.setStatus(0);
        bookingRepository.save(booking);
        return booking.getId();
    }

    /** Partial update — non-null fields only; the mobile cancel flow sends {@code status:-1}. */
    @Transactional
    public void update(BookingUpdateRequest request) {
        ParkingBooking booking = bookingRepository.findById(request.id())
                .orElseThrow(() -> new ServiceException("预约记录不存在"));
        if (request.carPlate() != null) {
            booking.setCarPlate(request.carPlate());
        }
        if (request.park() != null) {
            booking.setPark(request.park());
        }
        if (request.bookingDate() != null) {
            booking.setBookingDate(request.bookingDate());
        }
        if (request.cellPhone() != null) {
            booking.setCellPhone(request.cellPhone());
        }
        if (request.remark() != null) {
            booking.setRemark(request.remark());
        }
        if (request.status() != null) {
            booking.setStatus(request.status());
        }
        bookingRepository.save(booking);
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

    private static LocalDate parseDate(String bookingDate) {
        if (!hasText(bookingDate)) {
            return null;
        }
        try {
            return LocalDate.parse(bookingDate.trim());
        } catch (DateTimeParseException e) {
            throw new ServiceException("预约日期格式错误，应为 yyyy-MM-dd");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
```

- [ ] **Step 4: Run unit tests to verify they pass**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home && cd $HOME/projects/car-park/car-park-server && ./mvnw test -Dtest=BookingServiceTest`
Expected: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0

---

### Task 3: Web Controller and Security Configuration

**Files:**
- Modify: `car-park-server/src/main/java/press/mizhifei/parking/web/BookingController.java:63-68`
- Modify: `car-park-server/src/main/java/press/mizhifei/parking/security/SecurityConfig.java:54-62`

**Interfaces:**
- Produces: `POST /parking/booking` returning `R<Long>`, anonymous access to `GET /parking/booking/{id}`

- [ ] **Step 1: Update `BookingController.java`**

Change `add` endpoint return type from `R<Void>` to `R<Long>`:

```java
    /** Public, rate-limited. Server validates availability/invariants and sets {@code status=0}. */
    @PostMapping
    public R<Long> add(@Valid @RequestBody BookingCreateRequest request) {
        Long bookingId = bookingService.create(request);
        return R.ok(bookingId);
    }
```

- [ ] **Step 2: Update `SecurityConfig.java`**

Permit anonymous `GET /parking/booking/*` (covers `/parking/booking/{id}`, `/parking/booking/list`, and `/parking/booking/last`):

```java
                        .requestMatchers(HttpMethod.GET,
                                "/parking/park/list",
                                "/parking/park/leftLotNum",
                                "/parking/booking/list",
                                "/parking/booking/last",
                                "/parking/booking/*").permitAll()
```

- [ ] **Step 3: Run all server tests**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home && cd $HOME/projects/car-park/car-park-server && ./mvnw test`
Expected: Tests run: 8 (1 context-load + 7 service tests), Failures: 0, Errors: 0

---

### Task 4: Mobile Frontend Updates for Stable Booking ID

**Files:**
- Modify: `car-park-mobile/src/api/parking.js`
- Modify: `car-park-mobile/src/views/BookingAdd.vue`
- Modify: `car-park-mobile/src/views/BookingSuccess.vue`

**Interfaces:**
- Produces: `getBooking(id)` API call, `router.push('/success?id=...')`, and direct fetch in `BookingSuccess.vue`

- [ ] **Step 1: Update `car-park-mobile/src/api/parking.js`**

Add `getBooking` function:

```javascript
/**
 * Single booking record by ID.
 * @param {number|string} id
 */
export const getBooking = (id) => unwrap(http.get(`/parking/booking/${id}`))
```

- [ ] **Step 2: Update `car-park-mobile/src/views/BookingAdd.vue`**

Pass returned `id` to the success route:

```javascript
    const res = await addBooking({
      carPlate: form.carPlate,
      cellPhone: form.cellPhone,
      park: form.park,
      bookingDate: form.bookingDate,
    })
    const bookingId = res?.data
    showSuccessToast('预约成功')
    setTimeout(() => router.push(bookingId ? { path: '/success', query: { id: bookingId } } : '/success'), 600)
```

- [ ] **Step 3: Update `car-park-mobile/src/views/BookingSuccess.vue`**

Fetch booking by ID when `route.query.id` is available, with fallback to `getLastBooking(cellPhone)`:

```vue
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { showFailToast, showSuccessToast } from 'vant'
import { getBooking, getLastBooking, updateBooking } from '@/api/parking'
import { PHONE_STORAGE_KEY } from '@/constants'

const route = useRoute()
const booking = ref({})
const submitting = ref(false)
const qrDataUrl = ref('')

const cancelled = computed(() => booking.value.status === -1)

/**
 * QR payload — the JSON keys are the staff scanner's contract:
 * {车牌号, 手机号, 停车场, 日期}. Do NOT rename or reorder the semantics.
 */
function buildQrInfo({ carPlate, cellPhone, park, bookingDate }) {
  return JSON.stringify({
    车牌号: carPlate,
    手机号: cellPhone,
    停车场: park,
    日期: bookingDate,
  })
}

async function refreshQr() {
  if (!booking.value.id) {
    qrDataUrl.value = ''
    return
  }
  try {
    qrDataUrl.value = await QRCode.toDataURL(buildQrInfo(booking.value), {
      width: 200,
      margin: 1,
    })
  } catch {
    qrDataUrl.value = ''
  }
}

async function loadBooking() {
  const bookingId = route.query.id
  if (bookingId) {
    try {
      const res = await getBooking(bookingId)
      booking.value = res?.data || res || {}
      await refreshQr()
      return
    } catch {
      // Fall through to phone-based lookup on error
    }
  }

  const cellPhone = localStorage.getItem(PHONE_STORAGE_KEY)
  if (!cellPhone) return
  try {
    const last = await getLastBooking(cellPhone)
    booking.value = last || {}
    await refreshQr()
  } catch {
    showFailToast('获取预约记录失败，请稍后再试')
  }
}

async function cancelBooking() {
  const previousStatus = booking.value.status
  booking.value.status = -1 // optimistic, matches the legacy UI behavior
  submitting.value = true
  try {
    await updateBooking({ id: booking.value.id, status: -1 })
    showSuccessToast('取消成功')
  } catch (err) {
    // recovery: restore UI state and refetch the truth from the server
    booking.value.status = previousStatus
    showFailToast(err?.message || '取消失败，请稍后再试')
    await loadBooking()
  } finally {
    submitting.value = false
  }
}

onMounted(loadBooking)
</script>
```

- [ ] **Step 4: Build and lint mobile frontend**

Run: `cd $HOME/projects/car-park/car-park-mobile && deno task build && deno lint`
Expected: Build succeeds and 0 lint errors.

---

### Task 5: Domain Context Documentation

**Files:**
- Create: `CONTEXT.md`

**Interfaces:**
- Documents the domain model, glossary, and business invariants established during deepening.

- [ ] **Step 1: Write `CONTEXT.md`**

```markdown
# Domain Context: Car Park Booking System (停车场预约管理系统)

## Overview

A visitor parking-booking system for community parking lots. Citizens book slots from a public mobile H5 page (no login required) and present a QR code at the gate; staff manage lots and bookings via an admin console.

## Glossary

- **Park (停车场)**: A physical parking facility with a configured total capacity (`lotNum`), date window (`openDateBegin`, `openDateEnd`), and daily operating hours (`openTime`, `closeTime`). The park `name` is the join key across bookings.
- **Booking (停车预约)**: A reservation by a citizen for a single vehicle on a specific calendar date (`bookingDate`) at a named park. Status: `0` = 预约成功 (active), `-1` = 已取消 (cancelled).
- **Vehicle (车牌号)**: Chinese vehicle plate (ordinary 7-char or new-energy 8-char format).
- **Available Capacity (剩余车位 / leftLotNum)**: Computed dynamically in real-time as `lotNum - count(active bookings on that date)`. Never persisted.
- **Gate QR Contract**: JSON string with exact Chinese keys `{"车牌号": carPlate, "手机号": cellPhone, "停车场": park, "日期": bookingDate}` scanned by gate staff.

## Invariants

1. **Intake Concurrency & Capacity**: A park row is locked with `PESSIMISTIC_WRITE` during booking creation. Active booking count for that date cannot exceed `lotNum`.
2. **Date Validity**: Booking date cannot be in the past (`< LocalDate.now()`), and must fall within the park's `[openDateBegin, openDateEnd]` range if configured.
3. **Vehicle Exclusivity**: A vehicle (`carPlate`) can have at most one active (`status = 0`) booking per calendar date across all parking lots.
4. **Stateless JWT**: Admin users authenticate with stateless JWTs (720 min expiry). Anonymous surface is strictly restricted to citizen H5 booking workflows.
```

- [ ] **Step 2: Verify documentation consistency**

Verify that `CONTEXT.md` exists and matches `docs/agents/domain.md` expectations.
