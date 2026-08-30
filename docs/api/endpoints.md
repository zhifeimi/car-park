# REST API Endpoint Reference

## Authentication (`/auth`)

| Method | Endpoint | Access | Description | Request Body | Response |
|---|---|---|---|---|---|
| `POST` | `/auth/login` | **Public** | Staff login | `LoginRequest` `{username, password}` | `R<LoginResponse>` `{token, user}` |
| `GET` | `/auth/me` | Staff (JWT) | Get current user info | — | `R<User>` |
| `POST` | `/auth/password` | Staff (JWT) | Change password | `ChangePasswordRequest` `{oldPassword, newPassword}` | `R<void>` |
| `GET` | `/auth/users` | Staff (JWT) | List admin users | — | `R<User[]>` |
| `POST` | `/auth/users` | Staff (JWT) | Create admin user | `User` `{username, password, nickname, ...}` | `R<void>` |
| `DELETE` | `/auth/users/{id}` | Staff (JWT) | Delete admin user | — | `R<void>` |

---

## Parking Lots (`/parking/park`)

| Method | Endpoint | Access | Description | Parameters / Body | Response |
|---|---|---|---|---|---|
| `GET` | `/parking/park/list` | **Public** | List all parking lots | `ParkCriteria` `?name=&status=` | `TableData<Park>` |
| `GET` | `/parking/park/leftLotNum` | **Public** | Query available capacity | `?park={name}&date={YYYY-MM-DD}` | `R<number \| null>` |
| `GET` | `/parking/park/{id}` | Staff (JWT) | Get parking lot details | `id` in path | `R<Park>` |
| `POST` | `/parking/park` | Staff (JWT) | Add new parking lot | `Park` `{name, lotNum, status, ...}` | `R<void>` |
| `PUT` | `/parking/park` | Staff (JWT) | Update parking lot | `Park` `{id, name, lotNum, status, ...}` | `R<void>` |
| `DELETE` | `/parking/park/{ids}` | Staff (JWT) | Delete parking lot(s) | `ids` (comma-separated or single) | `R<void>` |

---

## Bookings (`/parking/booking`)

| Method | Endpoint | Access | Description | Parameters / Body | Response |
|---|---|---|---|---|---|
| `GET` | `/parking/booking/list` | **Public** | List bookings (filterable) | `BookingCriteria` `?name=&carPlate=&cellPhone=&bookingDate=&status=` | `TableData<ParkingBooking>` |
| `GET` | `/parking/booking/last` | **Public** | Get latest booking by phone | `?cellPhone={11-digit-phone}` | `R<ParkingBooking \| null>` |
| `POST` | `/parking/booking` | **Public** (Rate Limited: 10/min/IP) | Citizen booking submission | `IntakeBookingRequest` `{name, carPlate, cellPhone, bookingDate, remark}` | `R<void>` |
| `PUT` | `/parking/booking` | **Public** | Citizen cancellation | `CancelBookingRequest` `{id, status: -1}` | `R<void>` |
| `DELETE` | `/parking/booking/{ids}` | Staff (JWT) | Delete booking(s) | `ids` in path | `R<void>` |
| `GET` | `/parking/booking/statisticInfo` | Staff (JWT) | Aggregate booking stats | `?date={YYYY-MM-DD HH:mm:ss}` | `R<BookingStatistics>` |
