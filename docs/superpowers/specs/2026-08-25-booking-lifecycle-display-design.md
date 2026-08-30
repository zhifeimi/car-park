# Booking Lifecycle Display Design

**Status:** Approved design — awaiting written-spec review  
**Date:** 2026-08-25

## Purpose

Give the Booking Lifecycle State Machine a single cross-channel front-end presentation module. Today, the admin booking list, admin dashboard, and citizen Booking success page independently interpret raw status values, use duplicated label pairs, and disagree on null status.

The design deepens that knowledge behind one small, pure `@car-park/utils` interface. It does **not** alter the Booking Lifecycle State Machine, the frozen cancellation transport, the Gate QR payload, routes, envelopes, schema, or backend behavior.

## Constraints

- The server wire codes remain `0` for `ACTIVE` and `-1` for `CANCELLED`.
- `PUT /parking/booking` cancellation remains byte-equivalent to `{ id, status: -1 }` and stays outside this display-only module.
- Gate QR payload stays exactly `JSON.stringify({车牌号,手机号,停车场,日期})`.
- Only the three approved consumers migrate in this change:
  1. `apps/web/src/views/booking/index.vue`
  2. `apps/web/src/views/dashboard/index.vue`
  3. `apps/mobile/src/views/BookingSuccess.vue`
- `packages/utils` stays framework-free: no Vue, Element Plus, Vant, browser, or locale-store dependency.
- The public `BOOKING_STATUS` raw-code export is retired atomically with the consumer migration.
- Admin filter values remain literal numeric wire values `0` and `-1`; they are query/export adapter details, not display classification.

## Module

Create a focused pure TypeScript module at:

```text
packages/utils/src/booking-lifecycle-display.ts
```

Re-export it through the stable package entry:

```text
packages/utils/src/index.ts
```

The package entry remains the seam used by both applications. The focused source file gives lifecycle display rules locality and a direct Deno test surface.

### Presentation result

The module accepts a raw status and explicit locale, then returns one complete, framework-neutral result:

```ts
export type BookingDisplayLocale = 'zh-CN' | 'en'
export type BookingDisplayState = 'active' | 'cancelled' | 'unavailable'
export type BookingDisplayTreatment = 'positive' | 'neutral'

export interface BookingLifecycleDisplay {
  state: BookingDisplayState
  label: string
  treatment: BookingDisplayTreatment
  canShowQr: boolean
  canShowCancellationControl: boolean
}
```

The function must use strict equality for status classification:

| Raw status | State | 中文 | English | Treatment | QR / cancellation control |
|---|---|---|---|---|---|
| `0` | `active` | `预约成功` | `Confirmed` | `positive` | visible |
| `-1` | `cancelled` | `已取消` | `Cancelled` | `neutral` | hidden |
| `null`, `undefined`, strings, `NaN`, other values | `unavailable` | `状态不可用` | `Status unavailable` | `neutral` | hidden |

`locale` is explicit. `zh-CN` selects Chinese; the existing English locale is the fallback for unexpected runtime values.

The result exposes **semantic** treatment only. The web adapter maps it to Element Plus treatment; the mobile adapter decides its own visual presentation. No UI-library token or import crosses the shared seam.

## Status reachability

`status: null` can reach a client because the JPA converter permits null. It must render as `unavailable` in all three consumers.

A non-null, unsupported database code is rejected during backend entity conversion before a Booking reaches the client. That remains unchanged: the existing fetch-failure UI handles it. The pure display module nevertheless classifies defensive non-standard inputs as `unavailable`, making its behavior total and safe for direct callers and tests.

## Consumer migration

### Admin booking list

- Replace the row status ternaries with the shared presentation result.
- Map `positive` / `neutral` to the local Element Plus tag treatment.
- Use the shared canonical label for the tag text.
- Keep filter and export query values exactly numeric `0` and `-1`.
- The authenticated hard-delete action remains unrelated to cancellation and unchanged.
- Remove the now-unused duplicate `booking.statusSuccess` and `booking.statusCancelled` locale keys from both web dictionaries.

### Admin dashboard

- Replace the recent-Booking status ternary with the shared presentation result.
- Map semantic treatment locally to the existing Element Plus tag treatment.
- Use the shared canonical label.
- Leave dashboard aggregate cards and `statisticInfo` untouched.
- Remove the now-unused duplicate `dashboard.bookingSuccess` and `dashboard.cancelled` locale keys from both web dictionaries.

### Citizen Booking success page

Derive a reactive presentation result from the loaded Booking status and current locale.

- **Active:** show canonical confirmation label, QR, and cancellation control.
- **Cancelled:** show the canonical cancelled label; hide QR and cancellation control.
- **Unavailable:** show the canonical unavailable label; hide QR and cancellation control.
- On optimistic cancellation, the existing assignment to `status = -1` remains unchanged. The reactive display must hide the QR and control immediately.
- On cancellation failure, preserve the existing rollback and `/last` refetch path; the display converges to server truth.
- Keep the existing cancellation request literal and its frozen payload unchanged.
- Guard QR generation and rendering with the presentation result. The generated payload, QR library, and key names do not change.
- Keep action copy, toasts, fetch failures, and notices app-local. Delete only mobile locale keys made unused by replacing the page status mark/button state wording with canonical labels.

An active Booking for a past date may still show the cancellation control as it does today; the server remains the authority that rejects an ineligible cancellation. Date-aware cancellation eligibility is out of scope.

## Non-goals

- No backend converter leniency for unsupported non-null statuses.
- No new endpoint, route, schema, index, authorization, rate-limit, or envelope behavior.
- No QR payload change or scanner behavior change.
- No ownership or security change for anonymous cancellation.
- No mobile view-test framework.
- No cleanup of unrelated Vite aliases or retired-package references.
- No refactor of the cancellation request into a mobile transport module.

## Test and verification strategy

### Package behavior tests

Add `packages/utils/src/booking-lifecycle-display.test.ts` using the existing bare `Deno.test` and inline assertion convention.

Cover:

1. Exact label, state, treatment, and visibility result for active/cancelled/unavailable in both locales.
2. Strict classification: string values `'0'` and `'-1'`, nullish values, `NaN`, unexpected numbers, and non-numeric inputs all become `unavailable` without throwing.
3. Eligibility invariant: QR and cancellation control are available only for `active`.
4. Runtime-locale fallback resolves to English.
5. The public raw `BOOKING_STATUS` export is absent after the migration.

### Consumer verification

- `cd packages/utils && deno task test`
- `deno lint` from the repository root
- `cd apps/web && deno task test`
- `cd apps/web && deno task build`
- `cd apps/mobile && deno task build`
- `./scripts/test.sh`

After migration, review raw display comparisons with a narrow search. The admin filter values and mobile cancellation write are intentional exclusions because they implement frozen transport details rather than display classification.

A manual mobile smoke check verifies:

1. Active record: canonical active label, QR, and cancel control appear.
2. Successful cancellation: label switches immediately, QR/control disappear.
3. Failed cancellation: prior active presentation returns after rollback/refetch.
4. Cancelled record after reload: cancelled presentation with no QR/control.
5. Null status fixture: unavailable presentation with no QR/control.

## Acceptance criteria

- All three consumers obtain status presentation from the shared module.
- No caller-side raw-status display ternary remains.
- The old public raw status export has no in-repository consumer and is removed.
- Active, cancelled, and unavailable display behavior is identical in meaning across admin and citizen surfaces.
- QR visibility changes only; its payload contract is unchanged.
- Cancellation transport behavior remains byte-equivalent.
- Package tests, web tests, root lint, both frontend builds, and the repository test script pass.
