# Domain Model & Persistence

## Overview

The database schema (`carpark`) consists of three primary tables. `ddl-auto` is set to `none` to protect the live database structure.

## Database Tables

### 1. `cp_park` (Parking Lots)

Stores configured parking locations, capacities, and availability metadata.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `bigint` (PK, auto_increment) | No | Parking lot ID |
| `name` | `varchar(100)` | No | Parking lot unique name (join key) |
| `lot_num` | `int` | No | Total physical parking capacity |
| `status` | `tinyint` | No | Status (`0` = Normal, `1` = Disabled) |
| `remark` | `varchar(500)` | Yes | Optional notes |
| `create_by` | `varchar(64)` | Yes | Created by user/system |
| `create_time` | `datetime` | Yes | Creation timestamp |
| `update_by` | `varchar(64)` | Yes | Updated by user/system |
| `update_time` | `datetime` | Yes | Last update timestamp |

> **Note on `left_lot_num`**: Remaining capacity is **never persisted** as a column. It is dynamically computed via native SQL:
> `lot_num - count(active bookings for park + date)`.

---

### 2. `cp_parking_booking` (Visitor Bookings)

Records visitor reservation requests submitted by citizens via the H5 app or created by staff.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `bigint` (PK, auto_increment) | No | Booking record ID |
| `name` | `varchar(100)` | No | Parking lot name (joins to `cp_park.name`) |
| `car_plate` | `varchar(20)` | No | Vehicle plate number (standard/new-energy) |
| `cell_phone` | `varchar(20)` | No | 11-digit mobile phone number |
| `booking_date` | `date` | No | Reservation date (`YYYY-MM-DD`) |
| `status` | `tinyint` | No | Status: `0` (Success), `-1` (Cancelled) |
| `remark` | `varchar(500)` | Yes | Optional remarks |
| `create_by` | `varchar(64)` | Yes | Audit field (defaults to `"system"`) |
| `create_time` | `datetime` | Yes | Booking submission timestamp |
| `update_by` | `varchar(64)` | Yes | Audit field |
| `update_time` | `datetime` | Yes | Audit timestamp |

---

### 3. `cp_user` (Administrative Users)

Stores staff accounts authorized to access the admin console (`apps/web`).

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `bigint` (PK, auto_increment) | No | User ID |
| `username` | `varchar(50)` | No | Login username (unique) |
| `password` | `varchar(100)` | No | BCrypt hashed password |
| `nickname` | `varchar(50)` | Yes | Display name |
| `email` | `varchar(100)` | Yes | Email address |
| `cell_phone` | `varchar(20)` | Yes | Contact phone number |
| `status` | `tinyint` | No | `0` = Active, `1` = Disabled |
| `last_login_time` | `datetime` | Yes | Last successful login |
| `create_by` | `varchar(64)` | Yes | Creator |
| `create_time` | `datetime` | Yes | Creation timestamp |
| `update_by` | `varchar(64)` | Yes | Last updater |
| `update_time` | `datetime` | Yes | Update timestamp |

## JPA Auditing

All entities extend `BaseEntity`, mapped via `@MappedSuperclass`. The `AuditorAware<String>` bean provides the current username from Spring Security's `SecurityContextHolder` or falls back to `"system"` for unauthenticated requests.
