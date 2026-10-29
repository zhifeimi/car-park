/**
 * Gate QR contract payload — exact Chinese keys parsed by gate scanners.
 */
export interface QrPayload {
  车牌号: string
  手机号: string
  停车场: string
  日期: string
}

/**
 * Mainland China plate: ordinary 7-char and new-energy 8-char forms. Strict
 * client-side check for VITE_REGION=cn deployments; the server accepts the
 * wider PLATE_REGEX.
 */
export const CN_PLATE_REGEX =
  /^([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z](([0-9]{5}[DF])|([DF][A-HJ-NP-Z0-9][0-9]{4})))|([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳])$/

/** Mainland mobile number (11 digits starting with 13-19), VITE_REGION=cn. */
export const CN_MOBILE_REGEX = /^1[3-9]\d{9}$/

/**
 * Any country's plate, the server's rule (PLATE_REGEX in ParkingDtos; same
 * samples in validation.test.ts and ParkingDtosTest): letters of any script
 * and digits, single inner spaces / hyphens / middle dots, 2-16 characters.
 */
export const PLATE_REGEX = /^[\p{L}\p{N}](?:[\p{L}\p{M}\p{N}]|[ \-·](?=[\p{L}\p{N}])){1,15}$/u

/** E.164 international number, VITE_REGION=intl. The server also accepts CN_MOBILE_REGEX. */
export const E164_REGEX = /^\+[1-9]\d{6,14}$/

/**
 * Plate as typed → canonical: NFC, separators (spaces, hyphens, middle dots)
 * removed, upper case — "ab-123", "AB 123" and "AB·123" are all "AB123".
 * The server stores and compares this spelling (Plates.canonical); keep in sync.
 */
export function normalizePlate(plate: string): string {
  return plate.normalize('NFC').replace(/[\s\-·]+/g, '').toUpperCase()
}

/** Phone as typed ("+44 7700 900123", "+1 (202) 555-0143") → "+447700900123". */
export function normalizePhone(phone: string): string {
  return phone.replace(/[\s\-().]/g, '')
}

/** LocalStorage key for remembering last used cell phone */
export const PHONE_STORAGE_KEY = 'cellPhone'

/** LocalStorage key for the plate of the last booking — /last needs phone + plate */
export const PLATE_STORAGE_KEY = 'carPlate'

/** Keyboard character definitions */
export const PROVINCES: readonly string[] =
  '京津冀晋蒙辽吉黑沪苏浙皖闽赣鲁豫鄂湘粤桂琼渝川贵云藏陕甘青宁新'.split('')

export const LETTERS_ROW_1: readonly string[] = 'ABCDEFGHJKL'.split('')
export const LETTERS_ROW_2: readonly string[] = 'MNPQRSTUVWXYZ'.split('')
export const DIGITS: readonly string[] = '1234567890'.split('')

export * from './booking-lifecycle-display.ts'
export * from './i18n.ts'
export * from './server-messages.ts'

/**
 * Generate QR code JSON string payload for gate scanners
 */
export function generateQrPayload(booking: {
  carPlate: string
  cellPhone: string
  park: string
  bookingDate: string
}): string {
  const payload: QrPayload = {
    车牌号: booking.carPlate,
    手机号: booking.cellPhone,
    停车场: booking.park,
    日期: booking.bookingDate,
  }
  return JSON.stringify(payload)
}

/**
 * Safely parse QR code scanner payload
 */
export function parseQrPayload(qrString: string): QrPayload | null {
  try {
    const data = JSON.parse(qrString)
    if (data && data.车牌号 && data.手机号 && data.停车场 && data.日期) {
      return data as QrPayload
    }
    return null
  } catch {
    return null
  }
}

/**
 * Format Date to YYYY-MM-DD or YYYY-MM-DD HH:mm:ss
 */
export function formatDateTime(
  dateInput: Date | string | number = new Date(),
  withTime = false,
): string {
  const d = new Date(dateInput)
  if (isNaN(d.getTime())) return ''
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  if (!withTime) {
    return `${year}-${month}-${day}`
  }
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * R-envelope contract: business routes answer HTTP 200 + { code, msg, data } /
 * { total, rows, code, msg }; code !== 200 is a business failure. This is the
 * single normalization point shared by the admin console and the mobile H5 —
 * each app's HTTP layer decides what to *do* with a failure (dialog, toast,
 * throw); both must *detect* it identically. Lenient: an absent or non-numeric
 * code normalizes to 200 (non-envelope responses are treated as success).
 */
export function envelopeOf(body: Record<string, unknown> | null | undefined): { code: number; msg?: string } {
  const code = typeof body?.code === 'number' ? body.code : 200
  return { code, msg: typeof body?.msg === 'string' ? body.msg : undefined }
}
