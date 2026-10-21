/**
 * Worldwide plate / phone rules. Same sample table as the server's
 * ParkingDtosTest; keep both in sync so client and server agree.
 */
import { CN_PLATE_REGEX, E164_REGEX, normalizePhone, normalizePlate, PLATE_REGEX } from './index.ts'

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message)
}

Deno.test('plates from any country pass, markup and bad spacing do not', () => {
  const ok = [
    '粤A12345', '粤AD12345', '粤Z1234港',
    'AB12 CDE', 'M-AB 1234', '7ABC123', 'ABC-1234',
    '品川 300 さ 12-34', '12가 3456', 'กข 1234', 'А123ВС 77',
  ]
  const bad = ['<script>', "AB12'", 'A&B1', 'A', '', ' AB12', 'AB12 ', 'AB  12', '-AB12', 'AB--12', 'ABCDEFGHIJKLMNOPQ']
  for (const p of ok) assert(PLATE_REGEX.test(p), `should accept ${p}`)
  for (const p of bad) assert(!PLATE_REGEX.test(p), `should reject ${p}`)
})

Deno.test('every mainland plate is also a worldwide plate', () => {
  for (const p of ['粤A12345', '京AD12345', '沪B1234学']) {
    assert(CN_PLATE_REGEX.test(p) && PLATE_REGEX.test(p), `${p} should pass both`)
  }
})

Deno.test('typed input normalizes to what the server accepts', () => {
  assert(normalizePlate('  ab12   cde ') === 'AB12 CDE', 'plate: trim, collapse, upper')
  assert(PLATE_REGEX.test(normalizePlate(' m-ab  1234')), 'normalized plate passes')
  assert(normalizePhone('+44 7700 900123') === '+447700900123', 'phone: strip spaces')
  assert(normalizePhone('+1 (202) 555-0143') === '+12025550143', 'phone: strip punctuation')
  assert(E164_REGEX.test(normalizePhone('+1 (202) 555-0143')), 'normalized phone passes')
  assert(!E164_REGEX.test(normalizePhone('(202) 555-0143')), 'no country code is rejected')
})
