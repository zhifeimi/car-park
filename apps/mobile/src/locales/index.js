import { computed, ref } from 'vue'
import { Locale } from 'vant'
import enUS from 'vant/es/locale/lang/en-US'
import zhCNLocale from 'vant/es/locale/lang/zh-CN'
import en from './en.js'
import zhCN from './zh-CN.js'

const STORAGE_KEY = 'carpark_locale'
const DEFAULT_LOCALE = 'en'

// To add a language: copy en.js to xx.js, translate it, and add one entry to
// each map below (the Vant file also needs deno.json + vite.config.js entries;
// see packages/ui/DESIGN.md).
const messages = {
  en,
  'zh-CN': zhCN,
}
const vantLocales = {
  en: ['en-US', enUS],
  'zh-CN': ['zh-CN', zhCNLocale],
}

/** Switcher entries, in the order of `messages`. */
export const localeOptions = Object.keys(messages).map((code) => ({
  code,
  label: messages[code].common.langShort,
}))

/** The phone's preferred languages → first supported locale (exact tag, then language). */
function detectLocale() {
  for (const tag of globalThis.navigator?.languages ?? []) {
    if (messages[tag]) return tag
    const sameLanguage = Object.keys(messages).find((code) => code.split('-')[0] === tag.split('-')[0])
    if (sameLanguage) return sameLanguage
  }
  return DEFAULT_LOCALE
}

const stored = localStorage.getItem(STORAGE_KEY)
const currentLocale = ref(messages[stored] ? stored : detectLocale())

function applyLocale(lang) {
  Locale.use(...vantLocales[lang])
  // lang drives screen-reader pronunciation and per-script font fallback
  document.documentElement.lang = lang
}

export function setLocale(lang) {
  if (messages[lang]) {
    currentLocale.value = lang
    localStorage.setItem(STORAGE_KEY, lang)
    applyLocale(lang)
  }
}

export function initLocale() {
  applyLocale(currentLocale.value)
}

export function getLocale() {
  return currentLocale.value
}

export function t(path, params) {
  const lang = currentLocale.value
  const dict = messages[lang] || messages[DEFAULT_LOCALE]
  const keys = path.split('.')
  let value = dict
  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = value[k]
    } else {
      // Fallback to English
      let fallbackValue = messages[DEFAULT_LOCALE]
      for (const fk of keys) {
        if (fallbackValue && typeof fallbackValue === 'object' && fk in fallbackValue) {
          fallbackValue = fallbackValue[fk]
        } else {
          return path
        }
      }
      value = fallbackValue
      break
    }
  }

  if (typeof value === 'string' && params && typeof params === 'object') {
    return value.replace(/\{(\w+)\}/g, (_, key) => {
      return params[key] !== undefined ? String(params[key]) : `{${key}}`
    })
  }

  return value ?? path
}

// Per-park branding, set at build time (see packages/ui/DESIGN.md).
// import.meta.env is absent under plain `deno test`.
const env = import.meta.env ?? {}

export const siteName = computed(() =>
  (currentLocale.value === 'en' && env.VITE_SITE_NAME_EN) || env.VITE_SITE_NAME || '',
)

export function useI18n() {
  return {
    locale: currentLocale,
    setLocale,
    t,
    siteName,
  }
}
