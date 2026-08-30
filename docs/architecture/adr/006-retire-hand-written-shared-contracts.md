# ADR 006: Retire Hand-Written Shared Contract Packages

## Status
Accepted (2026-08-23)

## Context

ADR-004 established the monorepo layout with four shared packages, among them
`packages/types` (hand-written TypeScript definitions of domain entities, DTOs,
and API envelopes) and `packages/api-client` (a hand-written isomorphic typed
client). Both were written aspirationally: at retirement they had **zero
consumers** — neither `apps/web` nor `apps/mobile` imported either package, and
the only cross-package edge was `packages/utils` importing the 5-line
`QrPayload` interface.

Worse than dead weight, the hand-written contract had drifted from the live
wire contract it claimed to describe:

- `ParkingBooking.name` / `IntakeBookingRequest.name` where the server reads
  `park` (and requires `cellPhone`, absent from the intake type);
- a `BookingCriteria` with `name` instead of the real criteria fields
  (`carPlate`/`park`/`bookingDate`/`cellPhone`/`status`);
- a phantom `ParkStatus` column with no counterpart in `Park.java`.

Any consumer adopting these types would have produced valid-looking requests
that fail server-side (e.g. `停车场不能为空`). The packages were a hypothetical
seam with zero adapters and a wrong interface — active misdirection for future
contributors and AI sessions, not shared code.

## Decision

1. **Delete `packages/types` and `packages/api-client`.** The backend's DTOs
   (`press.mizhifei.parking.web.dto`) remain the single source of contract
   truth; the server re-validates every field regardless.
2. **`QrPayload` moves into `packages/utils/src/index.ts`**, beside its only
   consumers (`generateQrPayload` / `parseQrPayload`).
3. **If a typed client or shared types are ever genuinely needed, generate them
   from the backend's OpenAPI document** (dev profile serves it at
   `/v3/api-docs`) — never hand-write them again. A generated artifact cannot
   silently drift; a hand-written one always does.

## Consequences

- Two fewer workspace members and import-map entries (root + both apps'
  `deno.json`); two fewer packages to audit.
- The false seam is gone: contributors can't adopt wrong types because none
  exist.
- Pre-retirement sources preserved out-of-repo at
  `../car-park-shared-packages-backup-20260823.tar.gz`.
- If OpenAPI generation is later adopted, wire it into the build deliberately
  and re-examine this ADR.
