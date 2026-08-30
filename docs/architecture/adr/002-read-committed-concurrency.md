# ADR 002: READ COMMITTED Concurrency for Booking Intake

## Status
Accepted

## Context
High concurrency during booking intake risks overbooking when multiple visitors compete for limited slots. Under MySQL's default `REPEATABLE READ`, MVCC snapshot isolation prevents transactions from reading newly committed rows created after the transaction's first SELECT.

## Decision
Set `@Transactional(isolation = Isolation.READ_COMMITTED)` explicitly on `BookingService.intake`. Combine this with a pessimistic write lock (`PESSIMISTIC_WRITE`) on the target `Park` row during capacity verification.

## Consequences
- Guarantees serialized, accurate slot counting per parking lot.
- Eliminates phantom availability reads during intake races.
- Verified under concurrent multi-threaded stress tests.
