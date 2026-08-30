/**
 * Booking Lifecycle Display contract — strict-equality classification of the
 * raw wire status, localized labels (zh-CN exact, everything else English),
 * semantic treatment, and eligibility flags.
 */
import * as utils from './index.ts'
import { bookingLifecycleDisplay } from './index.ts'

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

Deno.test('six-row behavior matrix — every field of every row', () => {
  const matrix: ReadonlyArray<{
    status: unknown
    locale: string
    state: string
    label: string
    treatment: string
    eligible: boolean
  }> = [
    { status: 0, locale: 'zh-CN', state: 'active', label: '预约成功', treatment: 'positive', eligible: true },
    { status: 0, locale: 'en', state: 'active', label: 'Confirmed', treatment: 'positive', eligible: true },
    { status: -1, locale: 'zh-CN', state: 'cancelled', label: '已取消', treatment: 'neutral', eligible: false },
    { status: -1, locale: 'en', state: 'cancelled', label: 'Cancelled', treatment: 'neutral', eligible: false },
    { status: null, locale: 'zh-CN', state: 'unavailable', label: '状态不可用', treatment: 'neutral', eligible: false },
    { status: null, locale: 'en', state: 'unavailable', label: 'Status unavailable', treatment: 'neutral', eligible: false },
  ]
  for (const row of matrix) {
    const display = bookingLifecycleDisplay(row.status, row.locale)
    assert(display.state === row.state, `${row.status}/${row.locale}: expected state ${row.state}, got ${display.state}`)
    assert(display.label === row.label, `${row.status}/${row.locale}: expected label ${row.label}, got ${display.label}`)
    assert(display.treatment === row.treatment, `${row.status}/${row.locale}: expected treatment ${row.treatment}, got ${display.treatment}`)
    assert(display.canShowQr === row.eligible, `${row.status}/${row.locale}: expected canShowQr ${row.eligible}, got ${display.canShowQr}`)
    assert(display.canShowCancellationControl === row.eligible, `${row.status}/${row.locale}: expected canShowCancellationControl ${row.eligible}, got ${display.canShowCancellationControl}`)
  }
})

Deno.test('unsupported raw values all degrade to unavailable, never throw', () => {
  const unsupported: readonly unknown[] = ['0', '-1', undefined, NaN, 1, {}, []]
  for (const status of unsupported) {
    for (const locale of ['zh-CN', 'en']) {
      const display = bookingLifecycleDisplay(status, locale)
      assert(display.state === 'unavailable', `${String(status)}/${locale}: expected unavailable, got ${display.state}`)
      assert(display.canShowQr === false, `${String(status)}/${locale}: QR must stay hidden`)
      assert(display.canShowCancellationControl === false, `${String(status)}/${locale}: cancellation must stay hidden`)
      assert(
        display.label === (locale === 'zh-CN' ? '状态不可用' : 'Status unavailable'),
        `${String(status)}/${locale}: unexpected fallback label ${display.label}`,
      )
    }
  }
})

Deno.test('unexpected locale falls back to English', () => {
  assert(bookingLifecycleDisplay(0, 'fr').label === 'Confirmed', "locale 'fr' should fall back to English")
  assert(bookingLifecycleDisplay(-1, '').label === 'Cancelled', "empty locale should fall back to English")
  assert(bookingLifecycleDisplay(0, 'zh').label === 'Confirmed', "'zh' is not the zh-CN tag — falls back to English")
})

Deno.test('does not expose a raw public booking status object', () => {
  if ('BOOKING_STATUS' in utils) {
    throw new Error('raw BOOKING_STATUS must not remain part of the package interface')
  }
})
