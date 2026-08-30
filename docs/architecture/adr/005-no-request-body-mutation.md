# ADR 005: No Server-Side Request-Body Mutation

## Status
Accepted (2026-08-22)

## Context
The rewrite inherited the legacy system's XSS filter pair — `XssFilter` + `HtmlFilter`
(674 lines) — which intercepted every POST/PUT, read the raw JSON body, and ran a
PHP-era HTML-whitelisting regex pass over it before Jackson bound the payload. The
domain stores zero rich text: every string field is a car plate, phone number, park
name, or remark. Neither frontend renders stored text as HTML (no `v-html`, no
`innerHTML`); all output goes through Vue's auto-escaping as `application/json`.

The filter's observable effect was corruption, not protection: `1 < 2` typed into a
remark persisted as `1 &lt; 2`, and payloads like `<script>alert('xss')</script> 1<2 & 3>2`
bound as `alert('xss') 12` — silently destroyed content, invisible to every DTO,
service, and test because the transformation sat at a servlet-filter seam below them
all (`RequestBodyIntegrityTest` proves the contract; it was born red against the
filter and green after deletion).

## Decision
No server-side component mutates a request body before binding. What the client
submits is what the DTO binds. XSS defence lives where it is visible and testable:

1. **Input shape validation** — DTO `@Pattern`/`@Size` constraints reject anything
   that is not a legitimate value for the field (plates, phones, dates are
   grammar-constrained; free text is length-capped).
2. **Output-side escaping** — the frontends render all server data through Vue's
   auto-escaping; the API serves `application/json`, never HTML.

The filters, their `FilterConfig` registration, the `app.xss.*` configuration keys,
and their tests were deleted. `RequestBodyIntegrityTest` (default suite, no DB)
pins the contract: markup-bearing payloads must bind byte-identically at the
`BookingService` interface.

## Consequences
- Stored data equals submitted data; the entity-encoding corruption class is gone.
- Any future feature that legitimately stores rich text must reintroduce sanitizing
  as an explicit, per-field, visible step at the DTO/intake layer — never as a
  body-rewriting servlet filter — and update `RequestBodyIntegrityTest` consciously.
- Historical rows written while the filter ran may carry `&lt;`/`&gt;` mangling in
  `cp_park.name`/`remark`; `scripts/ops/xss-remediation.sql` audits and (after
  review) repairs them.
