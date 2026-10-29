import { computed, ref } from 'vue'
import enLocale from 'element-plus/es/locale/lang/en'
import zhCnLocale from 'element-plus/es/locale/lang/zh-cn'
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
// with the H5; this file only binds them to Vue and Element Plus.
const STORAGE_KEY = 'carpark_locale'
const DEFAULT_LOCALE = 'en'

// To add a language: copy en.js to xx.js, translate it, and add one entry to
// each map below (see packages/ui/DESIGN.md).
const messages = {
  en,
  'zh-CN': zhCN,
}
const elLocales = {
  en: enLocale,
  'zh-CN': zhCnLocale,
}

/** Dropdown entries, in the order of `messages`. */
export const localeOptions = optionsOf(messages)

const currentLocale = ref(initialLocale(STORAGE_KEY, Object.keys(messages), DEFAULT_LOCALE))

function applyLocale(lang) {
  // lang drives screen-reader pronunciation and per-script font fallback
  if (globalThis.document) document.documentElement.lang = lang
}
applyLocale(currentLocale.value)

export function setLocale(lang) {
  if (messages[lang]) {
    currentLocale.value = lang
    storeLocale(STORAGE_KEY, lang)
    applyLocale(lang)
  }
}

export function getLocale() {
  return currentLocale.value
}

export function t(path, params) {
  return translate(messages, DEFAULT_LOCALE, currentLocale.value, path, params)
}

export const elLocale = computed(() => elLocales[currentLocale.value])

// Per-park branding, set at build time (see packages/ui/DESIGN.md).
// import.meta.env is absent under plain `deno test`.
const env = import.meta.env ?? {}

export const siteName = computed(() => siteNameFor(currentLocale.value, env))

export function useI18n() {
  return {
    locale: currentLocale,
    setLocale,
    t,
    elLocale,
    siteName,
    localeOptions,
  }
}
