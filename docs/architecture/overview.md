# System Architecture Overview

## Overview

The CarPark visitor parking-booking system (停车场预约管理系统) is designed for the Huangma and Lingtou communities of Changling Street, Guangzhou. Citizens book visitor parking slots from a public mobile H5 page (no login required) and present a QR code at the community gate. Community staff manage parking lots, view and audit bookings, and monitor statistics via an administrative web console.

## Monorepo Structure

The project is organized as a unified monorepo:

```
my-project/
├── apps/
│   ├── web/                 # Admin console (Vue 3 + Element Plus + Vite on Deno)
│   ├── mobile/              # Citizen booking H5 (Vue 3 + Vant 4 + Vite on Deno)
│   └── backend/             # REST API server (Java 25 + Spring Boot 4.1 + Spring Data JPA)
│
├── packages/
│   ├── api-client/          # Shared isomorphic API Client SDK
│   ├── types/               # Shared TypeScript domain types, envelopes, and DTOs
│   ├── ui/                  # Shared Vue components (PlateKeyboard, BookingStatusBadge)
│   └── utils/               # Shared frontend utilities (validators, constants, QR helpers)
│
├── docs/
│   ├── architecture/        # System design, concurrency, Deno resolution, and ADRs
│   └── api/                 # REST endpoints, response envelopes, anonymous surface matrix
│
├── scripts/                 # Developer CLI utilities (dev, build, test, db-tunnel)
├── docker-compose.yml       # Local MySQL container setup
├── .github/
│   └── workflows/           # CI/CD pipelines (GitHub Actions)
├── README.md                # Project documentation & quick start
└── .gitignore               # Monorepo gitignore rules
```

## Technology Stack

| Layer | Component | Technology | Role |
|---|---|---|---|
| **Backend** | `apps/backend` | Java 25 (LTS), Spring Boot 4.1, Spring Data JPA, Hibernate 7, Spring Security, Jackson 3, MySQL 8+, HikariCP | REST API server with JWT authentication, rate limiting, and JPA auditing |
| **Web Admin** | `apps/web` | Vue 3.5, Element Plus, Pinia, Vue Router, Vite 8 on Deno 2.x | Staff administrative web console with zero `node_modules` |
| **Mobile H5** | `apps/mobile` | Vue 3.5, Vant 4, QRCode, Vue Router, Vite 8 on Deno 2.x | Public citizen booking single page application with zero `node_modules` |
| **Shared Types** | `packages/types` | TypeScript | Unified interfaces for domain models, DTOs, API envelopes (`R<T>`, `TableData<T>`) |
| **Shared Utils** | `packages/utils` | TypeScript | Chinese plate regex validation, phone validation, QR generator, formatters |
| **Shared UI** | `packages/ui` | Vue 3 SFC | Reusable plate keyboard and booking status badge components |
| **API Client** | `packages/api-client` | TypeScript | Configurable typed API client SDK for CarPark backend |

## System Data Flow

```
+------------------+         +--------------------+
|  Citizen Mobile  |         |  Staff Web Admin   |
|   (apps/mobile)  |         |    (apps/web)      |
+--------+---------+         +---------+----------+
         |                             |
         | HTTP (Public Surface)       | HTTP (JWT Auth)
         v                             v
+-------------------------------------------------+
|               apps/backend (:8080)              |
|  - RateLimiter (Bucket4j, in-memory per IP)     |
|  - JwtAuthenticationFilter                      |
|  - XSS Whitelist Filter                         |
|  - Spring MVC Controllers & REST Envelopes      |
|  - BookingService (READ COMMITTED intake logic) |
|  - ParkService (Availability calculation)       |
|  - AuthService (BCrypt password hashing)        |
+------------------------+------------------------+
                         |
                         | JDBC / HikariCP (SSL)
                         v
+-------------------------------------------------+
|                   MySQL 8+/9.7                  |
|  - cp_park (Parking lots & capacities)          |
|  - cp_parking_booking (Booking records)         |
|  - cp_user (Staff administrative accounts)      |
+-------------------------------------------------+
```
