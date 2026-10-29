/**
 * Framework-free locale plumbing shared by the admin console and the H5 —
 * each app's locales/index.js only adds its Vue refs and its UI library's
 * locale. Fix detection or lookup here, once.
 */

/** A locale's dictionary: nested objects of strings (or string arrays). */
export type Messages = Record<string, unknown>

/** Preferred languages (navigator.languages) → first supported locale: exact tag, then same language. */
export function detectLocale(
  supported: readonly string[],
  preferred: readonly string[] | undefined,
  fallback: string,
): string {
  for (const tag of preferred ?? []) {
    if (supported.includes(tag)) return tag
    const sameLanguage = supported.find((code) => code.split('-')[0] === tag.split('-')[0])
    if (sameLanguage) return sameLanguage
  }
  return fallback
}

/** The stored choice if still supported, else the detected one. Storage may throw (private mode). */
export function initialLocale(
  storageKey: string,
  supported: readonly string[],
  fallback: string,
): string {
  let stored: string | null = null
  try {
    stored = globalThis.localStorage?.getItem(storageKey) ?? null
  } catch {
    // unreadable storage → detect
  }
  if (stored && supported.includes(stored)) return stored
  // older WebViews expose only navigator.language
  const nav = globalThis.navigator
  const preferred = nav?.languages?.length ? nav.languages : nav?.language ? [nav.language] : []
  return detectLocale(supported, preferred, fallback)
}

/** Persist the choice; failures (private mode, quota) only lose the memory, never the switch. */
export function storeLocale(storageKey: string, lang: string): void {
  try {
    globalThis.localStorage?.setItem(storageKey, lang)
  } catch {
    // not persisted
  }
}

function lookup(dict: unknown, keys: string[]): unknown {
  let value = dict
  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = (value as Record<string, unknown>)[k]
    } else {
      return undefined
    }
  }
  return value
}

/** `a.b.c` in `lang`, falling back to `fallback`, then to the path itself; `{name}` params filled. */
export function translate(
  messages: Record<string, Messages>,
  fallback: string,
  lang: string,
  path: string,
  params?: Record<string, unknown>,
): unknown {
  const keys = path.split('.')
  let value = lookup(messages[lang] ?? messages[fallback], keys)
  if (value === undefined) value = lookup(messages[fallback], keys)
  if (value === undefined) return path
  if (typeof value === 'string' && params && typeof params === 'object') {
    return value.replace(/\{(\w+)\}/g, (_, key) => (params[key] !== undefined ? String(params[key]) : `{${key}}`))
  }
  return value ?? path
}

/** Per-park branding (packages/ui/DESIGN.md): VITE_SITE_NAME_EN for English, else VITE_SITE_NAME. */
export function siteNameFor(lang: string, env: Record<string, string | undefined>): string {
  return (lang === 'en' && env.VITE_SITE_NAME_EN) || env.VITE_SITE_NAME || ''
}

/**
 * Switcher entries in `messages` order, each labelled in its own language by
 * `common.<labelKey>` (the H5's compact switch uses a short label, the admin
 * dropdown the full name).
 */
export function localeOptions(
  messages: Record<string, Messages>,
  labelKey = 'languageName',
): { code: string; label: string }[] {
  return Object.keys(messages).map((code) => ({
    code,
    label: String(lookup(messages[code], ['common', labelKey]) ?? code),
  }))
}
