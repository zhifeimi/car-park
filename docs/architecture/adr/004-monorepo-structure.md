# ADR 004: Monorepo Architecture with Apps and Shared Packages

## Status
Accepted

## Context
Code duplication between admin and mobile apps (license plate validation regexes, mobile phone rules, QR payload contracts, API envelope definitions) created synchronization risks.

## Decision
Restructure the project into an organized monorepo layout:
- `apps/`: Deployable applications (`apps/web`, `apps/mobile`, `apps/backend`).
- `packages/`: Shared libraries (`packages/types`, `packages/utils`, `packages/ui`, `packages/api-client`).
- `docs/`: Centralized architecture and API documentation.
- `scripts/`: Shared CLI developer scripts.

## Consequences
- Single source of truth for TypeScript domain types and validation logic.
- Atomic updates across frontend and backend contracts.
- Seamless Deno workspace integration with zero `node_modules`.
