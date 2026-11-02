// deno test — Park.country helpers: the H5's plate-entry switch and the admin picker's list.
import { COUNTRY_CODES, countryName, isMainlandChina, MAINLAND_CHINA } from './country.ts'

function assert(condition: unknown, message = 'assertion failed'): asserts condition {
  if (!condition) throw new Error(message)
}

Deno.test('only a mainland-China lot gets the province plate keyboard', () => {
  assert(MAINLAND_CHINA === 'CN')
  assert(isMainlandChina('CN'))
  assert(isMainlandChina('cn'), 'case-insensitive')
  assert(isMainlandChina(' CN '), 'whitespace-tolerant')
  // the default: no lot picked yet, or a row from before the column existed
  assert(!isMainlandChina(undefined))
  assert(!isMainlandChina(null))
  assert(!isMainlandChina(''))
  // Hong Kong, Macao and Taiwan have codes (and plates) of their own
  for (const code of ['HK', 'MO', 'TW', 'GB', 'US', 'CHN']) {
    assert(!isMainlandChina(code), code)
  }
})

Deno.test('country list: ISO 3166-1 alpha-2, sorted, unique (ParkTest pins it to the JDK list)', () => {
  assert(COUNTRY_CODES.length === 249, `got ${COUNTRY_CODES.length}`)
  assert(COUNTRY_CODES.every((c) => /^[A-Z]{2}$/.test(c)))
  assert(new Set(COUNTRY_CODES).size === COUNTRY_CODES.length, 'duplicates')
  assert(COUNTRY_CODES.join() === [...COUNTRY_CODES].sort().join(), 'not sorted')
  for (const code of ['CN', 'HK', 'MO', 'TW', 'GB', 'US', 'DE', 'JP']) {
    assert(COUNTRY_CODES.includes(code), code)
  }
})

Deno.test('countryName localizes, and falls back to the code', () => {
  assert(countryName('DE', 'en') === 'Germany', countryName('DE', 'en'))
  assert(countryName('DE', 'zh-CN') === '德国', countryName('DE', 'zh-CN'))
  assert(countryName('DE', 'not a locale!') === 'DE', 'invalid locale → code')
})
