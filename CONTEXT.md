# Domain Context: Car Park Booking System (停车场预约管理系统)

## Overview

A visitor parking-booking system for community parking lots in the Huangma/Lingtou areas of Changling Street, Guangzhou. Citizens book slots from a public mobile H5 page (no login required) and present a QR code at the gate; staff manage lots and bookings via an admin console.

## Glossary

- **Park (停车场)**: A physical parking facility with a configured total capacity (`lotNum`), date window (`openDateBegin`, `openDateEnd`), and daily operating hours (`openTime`, `closeTime`). The park `name` is the join key across bookings.
- **Park Management & Schedule Invariants (停车场管理与运营时段规则)**: The deep domain module governing physical parking lot configuration, capacity definitions, operational scheduling windows, and unified query criteria (`ParkCriteria`). Enforces chronology invariants (`openDateBegin <= openDateEnd`, `openTime < closeTime`), capacity non-negativity (`lotNum >= 0`), and park name uniqueness across creation and partial updates.
- **Booking (停车预约)**: A reservation by a citizen for a single vehicle on a specific calendar date (`bookingDate`) at a named park.
- **Booking Lifecycle State Machine (预约生命周期状态机)**:
  - Encapsulated by `BookingStatus` (`ACTIVE` / `CANCELLED`) with JPA attribute conversion and semantic predicates (`isActive()`, `isCancellable()`).
  - `0` = 预约成功 (`BookingStatus.ACTIVE`): Occupies 1 capacity slot in `Available Capacity`, vehicle exclusivity enforced.
  - `-1` = 已取消 (`BookingStatus.CANCELLED`): Terminal state. Releases capacity slot immediately.
  - Valid transition: `0 -> -1` (`ParkingBooking.cancel(today)`). Cancellation is allowed only on active bookings for current or future dates (`bookingDate >= LocalDate.now()`).
  - Mutation & Cancellation Seam: Exposed via `cancel(Long id)` in the deep domain module. The sole public HTTP adapter is `PUT /parking/booking` with `status:-1` (`BookingUpdateRequest`); any other mutation payload is rejected with `不支持修改预约记录内容`.
- **Booking Lifecycle Display (预约生命周期展示)**: The cross-channel presentation of a Booking's lifecycle state to staff and citizens. When a Booking reaches a display without a recognised lifecycle status, it is shown neutrally as “status unavailable,” never as Cancelled. Only an Active Booking may present its Gate QR or cancellation control; a Cancelled Booking is presented explicitly as cancelled and never as a confirmation or a Gate QR, and status that is unavailable presents neither. Canonical lifecycle labels remain consistent across staff and citizen displays, while action-control wording may stay contextual. It conveys state only and does not perform or authorize lifecycle transitions.
- **Booking Intake & Operations (预约受理工单与运维)**: The unified module governing atomic creation, cancellation, query, and validation pipelines for parking reservations. Enforces date validity, vehicle exclusivity, park availability windows, capacity locks, and state transitions. Exposes unified query criteria via the deep `BookingCriteria` value object, consolidating search filters (`carPlate`, `park`, `bookingDate`, `cellPhone`, `status`), date validation format invariants (`yyyy-MM-dd`), pagination bounds, and legacy sort ordering (`bookingDate DESC, createTime DESC, cellPhone ASC, createBy ASC, carPlate ASC`).
- **Vehicle (车牌号)**: Chinese vehicle plate (ordinary 7-char or new-energy 8-char format).
- **Available Capacity (剩余车位 / leftLotNum)**: Computed dynamically in real-time as `lotNum - count(active bookings on that date)`. Never persisted.
- **Admin Identity & Access (管理员身份与认证管理)**: The deep module consolidating administrative authentication, stateless JWT session issuance, credential security, and administrator user lifecycle management. Enforces self-deletion guards and last-admin deletion safety invariants.
- **Admin HTTP Client (管理端HTTP客户端)**: The web console's single transport adapter (`apps/web/src/api/http.js`, ADR-007): every api function resolves to domain data (`{rows,total}` lists, objects, `void` mutations), rejects with `Error(server msg)` on business failure, and absorbs envelope codes, the single-flight 401 re-login dialog, toasts, and `.xlsx` downloads into its implementation. Views never see `{code,msg,data}`. Pinned by `apps/web/tests/http-client.test.ts` through axios's adapter slot.
- **Tabular Report Export (报表流式导出)**: Deep reporting module streaming tabular datasets (parks, booking logs) directly to HTTP client output streams as styled `.xlsx` workbooks with zero intermediate heap byte buffering.
- **Gate QR Contract**: JSON string with exact Chinese keys `{"车牌号": carPlate, "手机号": cellPhone, "停车场": park, "日期": bookingDate}` scanned by gate staff.

## Invariants

1. **Intake Concurrency & Capacity**: A park row is locked with `PESSIMISTIC_WRITE` during booking creation. Active booking count for that date cannot exceed `lotNum`.
2. **Date Validity**: Booking date cannot be in the past (`< LocalDate.now()`), and must fall within the park's `[openDateBegin, openDateEnd]` range if configured.
3. **Vehicle Exclusivity**: A vehicle (`carPlate`) can have at most one active (`status = 0`) booking per calendar date across all parking lots. Enforced by an authoritative re-check under the park row lock with a fresh committed view (`intake` runs at READ COMMITTED); the same-park race is closed and proven by the `@Tag("db")` suite, while a residual microsecond window across *different* parks remains open.
4. **Cancellation Rules**: Cancellation is only permitted on active (`status = 0`) bookings for current or future dates (`bookingDate >= LocalDate.now()`). Past bookings or already-cancelled bookings cannot be cancelled.
5. **Mutation Restriction**: Public HTTP mutations on bookings are restricted strictly to cancellation. Arbitrary field modification without invariant validation is disallowed.
6. **Stateless JWT & Anonymous Surface**: Admin users authenticate with stateless JWTs (720 min expiry). The anonymous surface is exactly seven routes — `POST /auth/login`; `GET /parking/park/list`; `GET /parking/park/leftLotNum`; `GET /parking/booking/list`; `GET /parking/booking/last`; `POST /parking/booking`; `PUT /parking/booking` (plus `GET /actuator/health` for probes) — nothing else. The boundary is pinned by a route-matrix test that fails whenever any route's public/protected classification drifts from this list. Per-id booking reads do not exist: the citizen success page loads by phone via `/parking/booking/last`.
7. **Admin Safety Invariants**: An administrator cannot delete their own active account (`username.equals(currentUsername)`), and the system must prevent deletion of the last remaining administrator (`count <= 1`). Failed authentication attempts must return generic messages to avoid user-existence enumeration.
8. **Park Schedule Chronology & Validation**: When configured, `openDateBegin` cannot be after `openDateEnd` (`openDateBegin <= openDateEnd`), and daily operating `openTime` must be strictly before `closeTime` (`openTime < closeTime`). Date and time format parsing errors are rejected.
9. **Park Capacity Non-Negativity & Name Uniqueness**: Park total capacity `lotNum` cannot be negative (`>= 0`), and park `name` must be unique across all parking lots.
10. **Park Combined Partial Update Integrity**: Partial mutations on a park merge incoming changes onto existing entity state, ensuring that the combined post-mutation state satisfies all schedule and capacity invariants before persisting.
