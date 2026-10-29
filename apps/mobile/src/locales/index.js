import { computed, ref } from 'vue'
import { Locale } from 'vant'
import enUS from 'vant/es/locale/lang/en-US'
import zhCNLocale from 'vant/es/locale/lang/zh-CN'
import {
  initialLocale,
  localeOptions as optionsOf,
  siteNameFor,
  storeLocale,
  translate,
} from '@car-park/utils'
import en from './en.js'
import zhCN from './zh-CN.js'

// Detection, lookup and branding live in @car-park/utils (i18n.ts), shared
// with the admin console; this file only binds them to Vue and Vant.
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
export const localeOptions = optionsOf(messages, 'langShort')

const currentLocale = ref(initialLocale(STORAGE_KEY, Object.keys(messages), DEFAULT_LOCALE))

function applyLocale(lang) {
  Locale.use(...vantLocales[lang])
  // lang drives screen-reader pronunciation and per-script font fallback
  if (globalThis.document) document.documentElement.lang = lang
}

export function setLocale(lang) {
  if (messages[lang]) {
    currentLocale.value = lang
    storeLocale(STORAGE_KEY, lang)
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
  return translate(messages, DEFAULT_LOCALE, currentLocale.value, path, params)
}

// Per-park branding, set at build time (see packages/ui/DESIGN.md).
// import.meta.env is absent under plain `deno test`.
const env = import.meta.env ?? {}

export const siteName = computed(() => siteNameFor(currentLocale.value, env))

export function useI18n() {
  return {
    locale: currentLocale,
    setLocale,
    t,
    siteName,
  }
}
