/**
 * Drift guard: every fixed user-facing message the backend can send needs an
 * entry in SERVER_MESSAGES_EN, or visitors using English see Chinese toasts.
 * Messages built by concatenation ("缺少参数：" + name) are developer errors
 * and stay untranslated.
 */
import { localizeServerMessage, SERVER_MESSAGES_EN } from './server-messages.ts'

const JAVA_ROOT = new URL('../../../apps/backend/src/main/java/', import.meta.url)
const LITERAL = /(?:ServiceException\((?:\d+,\s*)?|R\.fail\((?:\d+,\s*)?|message\s*=\s*|\\"msg\\":\\")"?([^"\\]+)(?:"|\\")(?!\s*\+)/g

async function* javaFiles(dir: URL): AsyncGenerator<URL> {
  for await (const entry of Deno.readDir(dir)) {
    const url = new URL(entry.name + (entry.isDirectory ? '/' : ''), dir)
    if (entry.isDirectory) yield* javaFiles(url)
    else if (entry.name.endsWith('.java')) yield url
  }
}

Deno.test('every fixed backend message has an English translation', async () => {
  const missing = new Set<string>()
  let found = 0
  for await (const file of javaFiles(JAVA_ROOT)) {
    for (const [, msg] of (await Deno.readTextFile(file)).matchAll(LITERAL)) {
      if (!/\p{Script=Han}/u.test(msg)) continue
      found++
      if (!(msg in SERVER_MESSAGES_EN)) missing.add(msg)
    }
  }
  if (found < 20) throw new Error(`scanner found only ${found} messages; is LITERAL still right?`)
  if (missing.size) throw new Error(`Add to SERVER_MESSAGES_EN:\n${[...missing].join('\n')}`)
})

Deno.test('Chinese UI keeps the server text; other locales get English or the original', () => {
  const msg = '该停车场当日预约名额已满'
  if (localizeServerMessage(msg, 'zh-CN') !== msg) throw new Error('zh-CN must pass through')
  if (localizeServerMessage(msg, 'en') !== SERVER_MESSAGES_EN[msg]) throw new Error('en must translate')
  if (localizeServerMessage(msg, 'fr') !== SERVER_MESSAGES_EN[msg]) throw new Error('other locales fall back to English')
  if (localizeServerMessage('未知: 9', 'en') !== '未知: 9') throw new Error('unknown text passes through')
  if (localizeServerMessage(undefined, 'en') !== undefined) throw new Error('undefined stays undefined')
})
