import { ref } from 'vue'
import { Locale } from 'vant'
import enUS from 'vant/es/locale/lang/en-US'
import zhCNLocale from 'vant/es/locale/lang/zh-CN'
import en from './en.js'
import zhCN from './zh-CN.js'

const STORAGE_KEY = 'carpark_locale'
const DEFAULT_LOCALE = 'en'

const messages = {
  en,
  'zh-CN': zhCN,
}

// Global reactive locale state (default 'en')
const currentLocale = ref(localStorage.getItem(STORAGE_KEY) || DEFAULT_LOCALE)

function applyVantLocale(lang) {
  if (lang === 'zh-CN' || lang === 'zh') {
    Locale.use('zh-CN', zhCNLocale)
  } else {
    Locale.use('en-US', enUS)
  }
}

export function setLocale(lang) {
  if (messages[lang]) {
    currentLocale.value = lang
    localStorage.setItem(STORAGE_KEY, lang)
    applyVantLocale(lang)
  }
}

export function initLocale() {
  applyVantLocale(currentLocale.value)
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

export function useI18n() {
  return {
    locale: currentLocale,
    setLocale,
    t,
  }
}
