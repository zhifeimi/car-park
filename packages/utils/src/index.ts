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
 * 预约须知 — 社区出入通用指引 (Chinese & English)
 */
export const NOTICE_LINES_ZH: readonly string[] = [
  '1. 预约免费。入场时请向工作人员出示预约二维码，配合核验后有序停放。',
  '2. 请如实填写车牌号与手机号，信息不符的车辆将无法入场。',
  '3. 行程有变请及时取消预约，把车位让给有需要的居民。',
  '4. 车位余量实时统计，约满即止；开放时段以现场公告为准。',
]

export const NOTICE_LINES_EN: readonly string[] = [
  '1. Booking is free. Please present the QR code to staff upon entry and park in designated areas.',
  '2. Please provide accurate license plate and mobile number. Mismatched vehicles will be denied entry.',
  '3. If your schedule changes, please cancel your booking promptly to free up space for other residents.',
  '4. Parking availability is updated in real time; opening hours are subject to on-site announcements.',
]

export const NOTICE_LINES: readonly string[] = NOTICE_LINES_ZH

/**
 * Mainland China plate — ordinary 7-char and new-energy 8-char forms.
 * Must stay in sync with CN_PLATE_REGEX in the server DTOs
 * (press.mizhifei.parking.web.dto.ParkingDtos); the server re-validates.
 */
export const PLATE_REGEX =
  /^([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z](([0-9]{5}[DF])|([DF][A-HJ-NP-Z0-9][0-9]{4})))|([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳])$/

/** Mainland mobile number (11 digits starting with 13-19) */
export const MOBILE_REGEX = /^1[3-9]\d{9}$/

/** LocalStorage key for remembering last used cell phone */
export const PHONE_STORAGE_KEY = 'cellPhone'

/** Keyboard character definitions */
export const PROVINCES: readonly string[] =
  '京津冀晋蒙辽吉黑沪苏浙皖闽赣鲁豫鄂湘粤桂琼渝川贵云藏陕甘青宁新'.split('')

export const LETTERS_ROW_1: readonly string[] = 'ABCDEFGHJKL'.split('')
export const LETTERS_ROW_2: readonly string[] = 'MNPQRSTUVWXYZ'.split('')
export const DIGITS: readonly string[] = '1234567890'.split('')

export * from './booking-lifecycle-display.ts'

/**
 * Validate Chinese license plate number
 */
export function validateCarPlate(plate: string): boolean {
  if (!plate || typeof plate !== 'string') return false
  return PLATE_REGEX.test(plate.trim())
}

/**
 * Validate Chinese 11-digit mobile phone number
 */
export function validatePhone(phone: string): boolean {
  if (!phone || typeof phone !== 'string') return false
  return MOBILE_REGEX.test(phone.trim())
}

/**
 * Check if the plate is a New Energy vehicle plate (8 characters)
 */
export function isNewEnergyPlate(plate: string): boolean {
  if (!validateCarPlate(plate)) return false
  return plate.trim().length === 8
}

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
