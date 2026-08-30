import { computed, ref } from 'vue'
import enLocale from 'element-plus/es/locale/lang/en'
import zhCnLocale from 'element-plus/es/locale/lang/zh-cn'
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

export function setLocale(lang) {
  if (messages[lang]) {
    currentLocale.value = lang
    localStorage.setItem(STORAGE_KEY, lang)
  }
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

export const elLocale = computed(() => {
  return currentLocale.value === 'zh-CN' ? zhCnLocale : enLocale
})

export function useI18n() {
  return {
    locale: currentLocale,
    setLocale,
    t,
    elLocale,
  }
}
