# ADR 007: Admin Console HTTP Client Seam

## Status
Accepted (2026-08-24)

## Context
The admin console's transport logic was shallow: its modules' interfaces were as
complex as their implementations, and the wire contract leaked to every caller.
Three return conventions coexisted inside `src/api/` — `listPark`/`listBooking`/
`listUsers` handed back raw `{code,msg,data}`/`{total,rows}` envelopes,
`getPark`/`login`/`getMe`/`getStatisticInfo` unwrapped `.data`, mutations returned
envelopes nobody read — so a caller could not predict what a function resolved to
without reading its body. Every view unwrapped envelopes itself
(`res.rows || []`, `Number(res.total || 0)`), and `utils/download.js` was a third
HTTP path that re-created the axios config by hand (duplicating the baseURL
fallback and bearer header) outside the interceptor stack. UI side effects
(toasts) were entangled with i18n and the pinia store inside `utils/request.js`,
untested because nothing could construct that graph.

The mobile H5 already demonstrated the deepened shape (`mobile/src/api/parking.js`:
one `unwrap()`, domain objects out), proving a second adapter for the same
contract — the seam is real, not hypothetical. Surfaced by the 2026-08-24
architecture review (candidate 1).

Not in scope: ADR-006 retired the *cross-repo* hand-written typed client; this ADR
records an app-local client module whose interface returns domain objects — no
shared type package exists to drift.

## Decision
`apps/web/src/api/http.js` is the console's single transport adapter. Its
implementation absorbs: envelope unwrapping (R + TableData), business-failure
rejection with the server's message, the single-flight 401 re-login dialog,
network-error toasts, the bearer header, and `.xlsx` downloads with JSON-error
sniffing. Domain clients (`api/parking.js`, `api/auth.js`) compose it; every
exported function follows one contract:

- list functions resolve `{rows, total}`, taking the filter object verbatim;
- getters resolve the domain object (`getStatisticInfo(date?: Date)` owns the
  `yyyy-MM-dd HH:mm:ss` wire format);
- mutations resolve `void`;
- exports resolve the workbook Blob (the view saves it);
- any business failure rejects with `Error(server msg)`.

Views never see envelope shapes again. `utils/request.js` and `utils/download.js`
were deleted.

Deliberate choice: **UI side effects stay inside the client** (Q3 of the design
session). The rejected alternative — `createClient({ onUnauthorized, onError })`
wired in `main.js` — is the cleaner seam but serves no second adapter today; all
15 call sites want identical toast behavior and none catches errors itself.
Revisit if a silent consumer (background polling, offline queue) appears.

Testability is pinned through the interface: `tests/http-client.test.ts` swaps
transport via axios's adapter slot and observes side effects via the exported
`ui` hook bag (internal seam, used only by its own tests). It pins: TableData →
`{rows,total}`, rejection with the server message, single-flight 401, void
mutations, the statistics date format, and xlsx-vs-JSON download sniffing.

## Consequences
- Locality: envelope codes, re-auth, and download quirks change in one module.
- Leverage: new admin endpoints are one-line compositions over `request()`/
  `download()`; views shrink to presentation.
- The interface is the test surface: transport fakes replace network stubs;
  Element Plus is never imported by tests.
- `http.js` must use relative imports (`../stores/...`) and guard
  `import.meta.env` — the same file runs under Vite (app) and plain
  `deno test` (contract suite); `@/` aliases do not resolve in the latter.
- If a second behavioral profile for error handling ever emerges, promote the
  `ui` hook bag into explicit init-time wiring and update this ADR.
