# Booking Intake Concurrency Mechanism

## Problem Statement

When multiple citizens concurrently submit parking reservations for the same parking lot on the same date, a race condition can cause **overbooking** (allocating more bookings than the lot capacity) or duplicate active bookings for the same vehicle.

## Mechanism & Isolation Level

The intake transaction in `BookingService.intake` operates under **`READ COMMITTED`** isolation:

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void intake(IntakeBookingRequest request) { ... }
```

### Why `READ COMMITTED` Instead of MySQL's Default `REPEATABLE READ`?

Under MySQL's default `REPEATABLE READ` isolation level:
1. A transaction establishes an MVCC snapshot on its first read.
2. If Transaction A acquires a lock on the park row and Transaction B waits for that lock, Transaction B's snapshot was established *before* it obtained the lock.
3. Once Transaction A commits and Transaction B acquires the lock, Transaction B's subsequent SELECT queries still read from its initial snapshot. It would not see Transaction A's newly inserted booking row, causing an availability miscalculation and potential overbooking.

Under `READ COMMITTED`:
- Every SELECT statement reads the latest committed data from the database.
- Once Transaction B acquires the pessimistic write lock on the parking lot, its availability check accurately counts all bookings committed by concurrent transactions.

## Two-Phase Vehicle Exclusivity Check

1. **Fail-Fast Pre-Check (Before Lock Acquisition)**:
   - Queries `cp_parking_booking` for any existing active booking (`status = 0`) with the same `(carPlate, bookingDate)`.
   - Rejects early without holding locks if the vehicle is already booked.

2. **Pessimistic Lock & Authoritative Intake Check**:
   - Acquires a `PESSIMISTIC_WRITE` row lock on the target `Park` row:
     ```java
     Park park = parkRepository.findByNameForUpdate(request.getName());
     ```
   - Re-checks vehicle exclusivity under the lock.
   - Computes remaining lot capacity in native SQL:
     $$\text{leftLotNum} = \text{lotNum} - \text{count}(\text{active bookings for } (\text{park}, \text{bookingDate}))$$
   - If $\text{leftLotNum} \le 0$, throws `IllegalStateException("该停车场在所选日期车位已满")`.
   - Inserts the new booking record with `status = 0`.

## Verification Suite

This concurrency mechanism is verified by the real-MySQL integration test suite:
- `BookingIntakeIntegrationTest`: Runs concurrent thread pools simulating multiple vehicles competing for the last remaining slot.
- Tagged with `@Tag("db")` and runnable via `./mvnw test -Dsurefire.excludedGroups=`.
