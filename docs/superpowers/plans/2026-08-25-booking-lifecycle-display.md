# Booking Lifecycle Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace scattered Booking status display logic with a pure shared Booking Lifecycle Display module, then migrate the admin list, dashboard, and citizen success page without changing cancellation transport or the Gate QR payload.

**Architecture:** A focused TypeScript module in `packages/utils/src/` classifies raw status values strictly and returns one framework-neutral display result: canonical bilingual label, semantic treatment, and QR/cancellation-control eligibility. The package root re-exports that module. Web views adapt semantic treatment to Element Plus locally; the mobile view uses the eligibility flags to render an active-only QR and cancellation control.

**Tech Stack:** Deno 2, TypeScript, Vue 3 `<script setup>`, Element Plus, Vant 4, Vite 8, existing bare `Deno.test` utilities.

**Spec:** `docs/superpowers/specs/2026-08-25-booking-lifecycle-display-design.md`

## Global Constraints

- Preserve the Booking wire codes exactly: `0` is active and `-1` is cancelled.
- Preserve cancellation transport byte-for-byte: `PUT /parking/booking` receives `{ id, status: -1 }`; do not route it through the display module.
- Preserve the Gate QR JSON payload and its exact Chinese keys: `车牌号`, `手机号`, `停车场`, `日期`.
- The shared module must have no Vue, Element Plus, Vant, browser, or locale-store dependency.
- Migrate all three consumers atomically: admin booking list, admin dashboard, and mobile `BookingSuccess`.
- Only active status may render QR or a cancellation control. Cancelled and unavailable status render neither.
- Keep admin filter/export values numeric `0` and `-1`; they are frozen wire adapter details, not display classification.
- Retire the public `BOOKING_STATUS` export only after every current import has migrated.
- Do not touch Java code, routes, schema, `deno.json`, Vite configuration, request envelopes, or unrelated aliases.
- This directory is not a Git repository. Do not add commit commands or attempt commits.

---

## File Structure

| File | Responsibility |
|---|---|
| `packages/utils/src/booking-lifecycle-display.ts` | Focused pure classification, bilingual labels, semantic treatment, and display eligibility. |
| `packages/utils/src/booking-lifecycle-display.test.ts` | Direct Deno behavior matrix for the shared module. |
| `packages/utils/src/index.ts` | Stable package seam: re-export the focused module, then retire `BOOKING_STATUS` only after migration. |
| `apps/web/src/views/booking/index.vue` | Adapt shared display treatment to Element Plus while preserving numeric filter/export parameters. |
| `apps/web/src/views/dashboard/index.vue` | Adapt the shared result for recent Booking tags. |
| `apps/web/src/locales/{zh-CN,en}.js` | Remove web lifecycle labels made unused by the shared module. |
| `apps/mobile/src/views/BookingSuccess.vue` | Render lifecycle state consistently; QR and cancellation control exist only for active records. |
| `apps/mobile/src/locales/{zh-CN,en}.js` | Remove mobile lifecycle strings made unused by the shared canonical label. |

## Task 1: Create and lock down the pure shared module

**Files:**
- Create: `packages/utils/src/booking-lifecycle-display.ts`
- Create: `packages/utils/src/booking-lifecycle-display.test.ts`
- Modify: `packages/utils/src/index.ts`

**Interfaces:**

```ts
export type BookingDisplayLocale = 'zh-CN' | 'en'
export type BookingDisplayState = 'active' | 'cancelled' | 'unavailable'
export type BookingDisplayTreatment = 'positive' | 'neutral'

export interface BookingLifecycleDisplay {
  readonly state: BookingDisplayState
  readonly label: string
  readonly treatment: BookingDisplayTreatment
  readonly canShowQr: boolean
  readonly canShowCancellationControl: boolean
}

export function bookingLifecycleDisplay(
  status: unknown,
  locale: string,
): BookingLifecycleDisplay
```

- Consumed later by: both web views and `BookingSuccess.vue` through `@car-park/utils`.
- Compatibility during this task: retain `BOOKING_STATUS`; Task 3 removes it after its last consumer migrates.

- [x] **Step 1: Write failing root-seam tests**

Create `packages/utils/src/booking-lifecycle-display.test.ts` in the established bare-Deno style:

```ts
import { bookingLifecycleDisplay } from './index.ts'

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message)
}

Deno.test('maps the complete Chinese and English lifecycle matrix', () => {
  const cases = [
    [0, 'zh-CN', 'active', '预约成功', 'positive', true],
    [0, 'en', 'active', 'Confirmed', 'positive', true],
    [-1, 'zh-CN', 'cancelled', '已取消', 'neutral', false],
    [-1, 'en', 'cancelled', 'Cancelled', 'neutral', false],
    [null, 'zh-CN', 'unavailable', '状态不可用', 'neutral', false],
    [null, 'en', 'unavailable', 'Status unavailable', 'neutral', false],
  ] as const

  for (const [status, locale, state, label, treatment, eligible] of cases) {
    const display = bookingLifecycleDisplay(status, locale)
    assert(display.state === state, `${locale}/${String(status)} state mismatch`)
    assert(display.label === label, `${locale}/${String(status)} label mismatch`)
    assert(display.treatment === treatment, `${locale}/${String(status)} treatment mismatch`)
    assert(display.canShowQr === eligible, `${locale}/${String(status)} QR eligibility mismatch`)
    assert(display.canShowCancellationControl === eligible, `${locale}/${String(status)} cancellation eligibility mismatch`)
  }
})

Deno.test('classifies defensive non-wire values as unavailable without throwing', () => {
  for (const status of ['0', '-1', undefined, NaN, 1, {}, []]) {
    const display = bookingLifecycleDisplay(status, 'en')
    assert(display.state === 'unavailable', `expected unavailable for ${String(status)}`)
    assert(display.canShowQr === false, `QR must be hidden for ${String(status)}`)
    assert(display.canShowCancellationControl === false, `cancel must be hidden for ${String(status)}`)
  }
})

Deno.test('falls back to English for an unexpected runtime locale', () => {
  assert(
    bookingLifecycleDisplay(0, 'fr').label === 'Confirmed',
    'unexpected locale must use English fallback',
  )
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
cd packages/utils && deno task test
```

Expected: FAIL because `bookingLifecycleDisplay` is not exported by `./index.ts`.

- [x] **Step 3: Implement the focused module and re-export it**

Create `packages/utils/src/booking-lifecycle-display.ts`:

```ts
export type BookingDisplayLocale = 'zh-CN' | 'en'
export type BookingDisplayState = 'active' | 'cancelled' | 'unavailable'
export type BookingDisplayTreatment = 'positive' | 'neutral'

export interface BookingLifecycleDisplay {
  readonly state: BookingDisplayState
  readonly label: string
  readonly treatment: BookingDisplayTreatment
  readonly canShowQr: boolean
  readonly canShowCancellationControl: boolean
}

const labels = {
  'zh-CN': {
    active: '预约成功',
    cancelled: '已取消',
    unavailable: '状态不可用',
  },
  en: {
    active: 'Confirmed',
    cancelled: 'Cancelled',
    unavailable: 'Status unavailable',
  },
} as const

export function bookingLifecycleDisplay(
  status: unknown,
  locale: string,
): BookingLifecycleDisplay {
  const state: BookingDisplayState = status === 0
    ? 'active'
    : status === -1
      ? 'cancelled'
      : 'unavailable'
  const active = state === 'active'
  const dictionary = locale === 'zh-CN' ? labels['zh-CN'] : labels.en

  return {
    state,
    label: dictionary[state],
    treatment: active ? 'positive' : 'neutral',
    canShowQr: active,
    canShowCancellationControl: active,
  }
}
```

Add the root re-export to `packages/utils/src/index.ts`, retaining the old raw constant temporarily:

```ts
export * from './booking-lifecycle-display.ts'
```

- [x] **Step 4: Run focused verification**

Run:

```bash
cd packages/utils && deno task test
deno check src/index.ts src/booking-lifecycle-display.ts
```

Expected: PASS. All six locale/state combinations and defensive inputs pass; the focused module has zero external imports.

---

## Task 2: Migrate both admin display adapters and remove duplicate web labels

**Files:**
- Create: `apps/web/tests/booking-lifecycle-display.test.ts`
- Modify: `apps/web/src/views/booking/index.vue:20-23,41-45,65-71`
- Modify: `apps/web/src/views/dashboard/index.vue:98-100,109-115`
- Modify: `apps/web/src/locales/zh-CN.js:85-86,120-121`
- Modify: `apps/web/src/locales/en.js:85-86,119-121`

**Interfaces:**
- Consumes: `bookingLifecycleDisplay(status: unknown, locale: string): BookingLifecycleDisplay` from Task 1.
- Produces: framework-local Element Plus tag treatment. No raw-status display ternary remains in either admin view.

- [x] **Step 1: Add an app-context resolution test**

Create `apps/web/tests/booking-lifecycle-display.test.ts`:

```ts
import { bookingLifecycleDisplay } from '@car-park/utils'

Deno.test('web resolves the shared Booking Lifecycle Display seam', () => {
  const display = bookingLifecycleDisplay(-1, 'zh-CN')
  if (display.label !== '已取消' || display.treatment !== 'neutral') {
    throw new Error('web import-map resolution or shared cancelled display is wrong')
  }
})
```

- [x] **Step 2: Run the app-context test before editing the views**

Run:

```bash
cd apps/web && deno task test
```

Expected: PASS after Task 1. Existing tests must stay green before consumer migration.

- [x] **Step 3: Migrate the booking-list display while preserving wire filters**

In `apps/web/src/views/booking/index.vue`:

1. Import `bookingLifecycleDisplay` from `@car-park/utils`.
2. Change `const { t } = useI18n()` to `const { locale, t } = useI18n()`.
3. Add the framework-local adapter:

   ```js
   function displayFor(status) {
     return bookingLifecycleDisplay(status, locale.value)
   }

   function tagTypeFor(status) {
     return displayFor(status).treatment === 'positive' ? 'success' : 'info'
   }
   ```

4. Preserve `:value="0"` and `:value="-1"`, but set labels with `displayFor(0).label` and `displayFor(-1).label`.
5. Replace the row tag with:

   ```vue
   <el-tag :type="tagTypeFor(row.status)" round>
     {{ displayFor(row.status).label }}
   </el-tag>
   ```

Do not alter `queryParams.status`, `handleExport`, or `handleDelete`.

- [x] **Step 4: Migrate the dashboard recent-Booking adapter**

In `apps/web/src/views/dashboard/index.vue`:

1. Import `bookingLifecycleDisplay` from `@car-park/utils`.
2. Destructure `locale` from `useI18n()`.
3. Add the same `displayFor(status)` and `tagTypeFor(status)` adapter functions.
4. Replace the recent-Booking tag:

   ```vue
   <el-tag size="small" :type="tagTypeFor(item.status)" round>
     {{ displayFor(item.status).label }}
   </el-tag>
   ```

Do not change statistic aggregation, park availability, or loading logic.

- [x] **Step 5: Delete only lifecycle labels made unused by web callers**

Delete these keys from both `apps/web/src/locales/zh-CN.js` and `apps/web/src/locales/en.js`:

```text
dashboard.bookingSuccess
dashboard.cancelled
booking.statusSuccess
booking.statusCancelled
```

Preserve all actions, messages, placeholders, and errors.

- [x] **Step 6: Run web verification**

Run:

```bash
cd apps/web && deno task test
cd apps/web && deno task build
cd ../.. && deno lint
```

Expected: PASS. Build proves the Vue graph resolves the root package seam; lint catches stale imports and references.

---

## Task 3: Migrate citizen presentation and retire the public raw status export

**Files:**
- Modify: `apps/mobile/src/views/BookingSuccess.vue:24-37,54-88,108-123`
- Modify: `apps/mobile/src/locales/zh-CN.js:43-55`
- Modify: `apps/mobile/src/locales/en.js:43-55`
- Modify: `packages/utils/src/index.ts:52-60`
- Modify: `packages/utils/src/booking-lifecycle-display.test.ts`

**Interfaces:**
- Consumes: the Task 1 shared presentation result.
- Produces: active-only QR/control rendering; package root no longer exports `BOOKING_STATUS`.
- Preserves: `updateBooking({ id, status: -1 })`, optimistic `booking.value.status = -1`, QR payload creation, rollback, and `/last` refetch.

- [x] **Step 1: Add a failing assertion for raw-export retirement**

Extend `packages/utils/src/booking-lifecycle-display.test.ts`:

```ts
import * as utils from './index.ts'

Deno.test('does not expose a raw public booking status object', () => {
  if ('BOOKING_STATUS' in utils) {
    throw new Error('raw BOOKING_STATUS must not remain part of the package interface')
  }
})
```

- [x] **Step 2: Run package tests to verify the retirement assertion fails first**

Run:

```bash
cd packages/utils && deno task test
```

Expected: FAIL only at the raw-export assertion because Task 1 deliberately retained the constant until its mobile consumer migrates.

- [x] **Step 3: Bind `BookingSuccess` to the reactive display result**

In `apps/mobile/src/views/BookingSuccess.vue`:

1. Replace the `BOOKING_STATUS` import with `bookingLifecycleDisplay`, retaining `generateQrPayload` and `PHONE_STORAGE_KEY`.
2. Replace `cancelled` with:

   ```js
   const display = computed(() =>
     bookingLifecycleDisplay(booking.value.status, locale.value),
   )
   ```

3. Render the page mark from `display.label`.
4. Render QR guidance, QR image, and cancellation control only for active display eligibility:

   ```vue
   <div class="mark">{{ display.label }}</div>
   <template v-if="display.canShowQr">
     <div class="notice">{{ t('success.notice1') }}</div>
     <div class="notice">{{ t('success.notice2') }}</div>
     <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-image" alt="QR Code" />
   </template>
   <van-button
     v-if="display.canShowCancellationControl"
     type="primary"
     :disabled="submitting"
     class="cancel-button"
     round
     @click="cancelBooking"
   >
     {{ t('success.cancelBooking') }}
   </van-button>
   ```

5. Guard QR generation before payload construction:

   ```js
   if (!booking.value.id || !display.value.canShowQr) {
     qrDataUrl.value = ''
     return
   }
   ```

6. Keep both `status = -1` literals as frozen cancellation transport details. Preserve the existing rollback and `loadBooking()` failure path unchanged.

- [x] **Step 4: Remove unused mobile status copy and the raw package export**

Delete `success.mark` and `success.bookingCancelled` from both mobile locale dictionaries after template references are removed. Keep `success.cancelBooking`, cancellation toast/error strings, booking-add success toast, QR guidance, and fetch/no-record copy.

Delete the public `BOOKING_STATUS` export and its stale display-label documentation from `packages/utils/src/index.ts`. Leave no replacement raw-code export.

- [x] **Step 5: Run package, mobile, and lint verification**

Run:

```bash
cd packages/utils && deno task test
cd apps/mobile && deno task build
cd ../.. && deno lint
```

Expected: PASS. The package test proves raw-export retirement; the mobile build proves existing package-root resolution still works; lint catches stale imports and locale keys.

---

## Task 4: Run integrated regression and behavior gates

**Files:**
- Verify only: all Task 1–3 files plus the approved spec and plan.

**Interfaces:**
- Verifies: shared display seam, frozen cancellation transport, active-only QR rendering, and existing backend/web regression behavior.

- [x] **Step 1: Check for raw display comparisons left in Vue views**

Run:

```bash
cd $HOME/projects/car-park
grep -RInE --include='*.vue' '\.status\s*(===|!==)\s*-?[01]\b' apps/web/src apps/mobile/src
```

Expected: no output. This intentionally excludes numeric admin filter values and mobile optimistic/write assignment because neither classifies display state.

- [x] **Step 2: Check for retired web lifecycle label callers**

Run:

```bash
cd $HOME/projects/car-park
grep -RInE "booking\.statusSuccess|booking\.statusCancelled|dashboard\.bookingSuccess|dashboard\.cancelled" apps/web/src
```

Expected: no output.

- [x] **Step 3: Run the full automated gate**

Run:

```bash
cd $HOME/projects/car-park
cd packages/utils && deno task test
cd ../../apps/web && deno task test && deno task build
cd ../mobile && deno task build
cd ../..
deno lint
./scripts/test.sh
```

Expected: every command exits zero. `scripts/test.sh` covers backend default tests, root lint, package tests, and web regression tests; explicit frontend builds cover both Vite production graphs.

- [x] **Step 4: Run the manual citizen-flow smoke check**

1. Start the backend and mobile H5 with the repository scripts.
2. Load an active Booking and confirm canonical active label, QR guidance, QR, and cancellation control are visible.
3. Cancel it and confirm the canonical cancelled label appears immediately while QR guidance, QR image, and cancellation control disappear.
4. Force a failed cancellation response and confirm the prior active presentation returns after rollback and `/last` refetch.
5. Reload after a successful cancellation and confirm the cancelled presentation remains QR-free.
6. Confirm the package null-status case yields `状态不可用` / `Status unavailable`, neutral treatment, and false eligibility flags.

> **Execution note (2026-08-25):** Under the user's one-booking local/dev authorization, the flow passed at `http://localhost:5174`, the configured dev CORS origin. The earlier `http://127.0.0.1:5174` attempt was correctly rejected by the backend CORS allowlist and created no booking. Active, cancelled, reload, bilingual label, and null-status behavior were verified. No safe local cancellation-failure fixture exists; forcing that response would require a second active booking beyond the authorization, so the unchanged rollback/refetch path was source- and contract-verified rather than dynamically mutated.

- [x] **Step 5: Review final scope against the spec**

Confirm only the focused utils module/test, its root re-export, the three agreed consumers, and locale keys made unused by them changed. Confirm no Java file, route, schema, Vite/Deno configuration, QR payload utility, or cancellation request shape changed.
