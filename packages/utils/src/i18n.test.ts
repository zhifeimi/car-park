// deno test — shared locale plumbing (i18n.ts) used by apps/web and apps/mobile.
import { detectLocale, localeOptions, siteNameFor, translate } from './i18n.ts'

function assert(condition: unknown, message = 'assertion failed'): asserts condition {
  if (!condition) throw new Error(message)
}

const messages = {
  en: { common: { languageName: 'English', langShort: 'EN' }, greet: 'Hi {name}', only: { en: 'yes' } },
  'zh-CN': { common: { languageName: '简体中文', langShort: '中文' }, greet: '你好 {name}' },
}

Deno.test('detection: exact tag, then same language, then the fallback', () => {
  const supported = Object.keys(messages)
  assert(detectLocale(supported, ['zh-CN'], 'en') === 'zh-CN')
  assert(detectLocale(supported, ['zh-TW', 'en'], 'en') === 'zh-CN', 'same language wins over a later exact tag')
  assert(detectLocale(supported, ['fr-FR'], 'en') === 'en')
  assert(detectLocale(supported, undefined, 'en') === 'en')
})

Deno.test('translate: params, English fallback, then the path', () => {
  assert(translate(messages, 'en', 'zh-CN', 'greet', { name: 'A' }) === '你好 A')
  assert(translate(messages, 'en', 'zh-CN', 'only.en') === 'yes')
  assert(translate(messages, 'en', 'zh-CN', 'missing.key') === 'missing.key')
  assert(translate(messages, 'en', 'xx', 'greet', {}) === 'Hi {name}')
})

Deno.test('site name follows the locale', () => {
  const env = { VITE_SITE_NAME: '黄麻停车场', VITE_SITE_NAME_EN: 'Huangma Parking' }
  assert(siteNameFor('en', env) === 'Huangma Parking')
  assert(siteNameFor('zh-CN', env) === '黄麻停车场')
  assert(siteNameFor('en', { VITE_SITE_NAME: '黄麻停车场' }) === '黄麻停车场')
  assert(siteNameFor('en', {}) === '')
})

Deno.test('switcher labels come from each locale itself', () => {
  assert(localeOptions(messages).map((o) => o.label).join() === 'English,简体中文')
  assert(localeOptions(messages, 'langShort').map((o) => o.label).join() === 'EN,中文')
})
