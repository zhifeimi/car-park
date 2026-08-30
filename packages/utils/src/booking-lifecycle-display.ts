/**
 * Booking Lifecycle Display seam — pure mapping from the raw Booking wire
 * status code to display semantics (state, localized label, semantic
 * treatment, eligibility flags) shared by the admin console and mobile H5.
 *
 * Raw wire-code meaning is preserved verbatim from the backend:
 * numeric `0` = 预约成功 (active), numeric `-1` = 已取消 (cancelled).
 * Classification uses strict equality only — nullish values, strings, `NaN`,
 * unsupported numbers, and any other value degrade to `unavailable` without
 * throwing.
 *
 * Pure module: no Vue, Element Plus, Vant, browser, or locale-store
 * dependency. Only `zh-CN` selects Chinese; every unexpected locale falls
 * back to English. Treatment stays semantic (`positive`/`neutral`) — mapping
 * to UI-library tokens happens at the consumer.
 */

/** Locales the seam renders; unexpected locale tags resolve to English. */
export type BookingDisplayLocale = 'zh-CN' | 'en'

export type BookingDisplayState = 'active' | 'cancelled' | 'unavailable'

/** Semantic emphasis only — never a UI-library token. */
export type BookingDisplayTreatment = 'positive' | 'neutral'

export interface BookingLifecycleDisplay {
  readonly state: BookingDisplayState
  readonly label: string
  readonly treatment: BookingDisplayTreatment
  /** Active bookings only — the gate QR must not render otherwise. */
  readonly canShowQr: boolean
  /** Active bookings only — cancellation applies before entry. */
  readonly canShowCancellationControl: boolean
}

/** Canonical labels keyed by state, one dictionary per supported locale. */
const LABELS: Record<BookingDisplayLocale, Record<BookingDisplayState, string>> = {
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
}

function resolveLabels(locale: string): Record<BookingDisplayState, string> {
  return LABELS[locale === 'zh-CN' ? 'zh-CN' : 'en']
}

/** Strict-equality classification; anything unrecognised is unavailable. */
function classifyStatus(status: unknown): BookingDisplayState {
  if (status === 0) return 'active'
  if (status === -1) return 'cancelled'
  return 'unavailable'
}

/**
 * Map a raw booking status code plus a locale tag to its full display model.
 */
export function bookingLifecycleDisplay(
  status: unknown,
  locale: string,
): BookingLifecycleDisplay {
  const labels = resolveLabels(locale)
  const state = classifyStatus(status)
  const active = state === 'active'
  return {
    state,
    label: labels[state],
    treatment: active ? 'positive' : 'neutral',
    canShowQr: active,
    canShowCancellationControl: active,
  }
}
