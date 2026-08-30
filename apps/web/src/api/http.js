/**
 * HTTP client internals — the admin console's single transport adapter.
 *
 * Everything a caller must NOT need to know lives here: the axios instance
 * (baseURL /dev-api, bearer header), envelope unwrapping (R and TableData),
 * business-failure rejection with the server message, the single-flight 401
 * re-login dialog, network-error toasts, and .xlsx downloads with
 * JSON-error-payload sniffing.
 *
 * Interface every api module builds on:
 *   request(config) → domain data (envelope stripped)
 *   download(url, params) → workbook Blob (JSON error payload → thrown Error)
 *
 * `ui` is an internal seam: Element Plus side effects, swappable in tests.
 * Relative imports throughout — @/ is a Vite alias deno test can't resolve,
 * and this module must be testable through plain `deno task test`.
 */
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { envelopeOf } from '@car-park/utils'
import { pinia } from '../stores/pinia.js'
import { useUserStore } from '../stores/user.js'
import { t } from '../locales/index.js'

export const ui = {
  /** Business-failure toast. */
  message(text) {
    ElMessage.error(text)
  },
  /** Single-flight re-login confirm; resolves on confirm, rejects on dismiss. */
  reloginDialog() {
    return ElMessageBox.confirm(t('common.sessionExpired'), t('common.systemTip'), {
      confirmButtonText: t('common.relogin'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  },
  /** Network-level failure toast (HTTP errors, timeouts). */
  transportError(text) {
    ElMessage.error(text)
  },
}

// single-flight flag so concurrent 401s only open one dialog (legacy isRelogin.show)
let isRelogin = false

function handleUnauthorized() {
  if (!isRelogin) {
    isRelogin = true
    ui.reloginDialog()
      .then(() => {
        isRelogin = false
        useUserStore(pinia).logout()
        location.href = '/login'
      })
      .catch(() => {
        isRelogin = false
      })
  }
}

/** True when the response carries binary content rather than an envelope.
 *  Real adapters expose the request via res.request; the response's own
 *  config (always present, incl. custom adapters) is the primary signal. */
function isBinary(res) {
  const responseType = res.config?.responseType ?? res.request?.responseType
  return responseType === 'blob' || responseType === 'arraybuffer'
}

/** Extract the server's human message from any failure shape. */
function serverMsg(error) {
  return error?.response?.data?.msg ?? error?.message
}

function unwrapOrThrow(res) {
  const { code, msg: envelopeMsg } = envelopeOf(res.data)
  const msg = envelopeMsg || t('common.unknownError')
  if (code === 401) {
    handleUnauthorized()
    throw new Error(msg)
  }
  if (code !== 200) {
    ui.message(msg)
    throw new Error(msg)
  }
  return res.data
}

// Vite injects import.meta.env at build/dev time; under plain `deno test` it's
// absent — fall back to the dev prefix (the adapter seam overrides transport anyway).
const viteEnv = import.meta.env ?? {}

const service = axios.create({
  baseURL: viteEnv.VITE_APP_BASE_API || '/dev-api',
  timeout: 30000,
})

// Test seam: the adapter slot swaps transport for a fake (tests/http-client.test.ts).
export const http = service

service.interceptors.request.use((config) => {
  const userStore = useUserStore(pinia)
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

service.interceptors.response.use(
  (res) => {
    if (isBinary(res)) {
      return res
    }
    return unwrapOrThrow(res)
  },
  (error) => {
    let message = error.message
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
    }
    if (message === 'Network Error') {
      message = t('common.networkError')
    } else if (message.includes('timeout')) {
      message = t('common.timeoutError')
    } else if (message.includes('Request failed with status code')) {
      message = t('common.serverError', { status: status ?? '' })
    }
    const msg = serverMsg(error) || message
    ui.transportError(msg)
    return Promise.reject(new Error(msg))
  },
)

/**
 * Envelope-stripping request. Resolves to:
 *   - `{rows, total}` when the payload is TableData (rows defaulted, total numeric)
 *   - `data` when the payload carries an R envelope's data field
 *   - `undefined` for void envelopes (data null/undefined)
 *   - the body itself when it isn't an envelope
 */
export function request(config) {
  return service.request(config).then((body) => {
    if (Array.isArray(body?.rows)) {
      return { rows: body.rows, total: Number(body.total ?? 0) }
    }
    if (body !== null && typeof body === 'object' && typeof body.code === 'number') {
      return body.data ?? undefined
    }
    return body
  })
}

/**
 * Download transport. POSTs with responseType blob through the same instance
 * (bearer header comes from the shared request interceptor), sniffs JSON error
 * payloads (legacy blobValidate), and hands back the raw Blob — saving it
 * stays the caller's concern.
 */
export async function download(url, params) {
  let res
  try {
    res = await service.post(url, null, { params, responseType: 'blob' })
  } catch (err) {
    ui.transportError(serverMsg(err) || t('common.downloadFailed'))
    throw err
  }
  const contentType = res.headers['content-type'] || ''
  if (contentType.includes('application/json')) {
    let msg = t('common.downloadFailed')
    try {
      msg = JSON.parse(await res.data.text()).msg || msg
    } catch {
      // keep default message
    }
    throw new Error(msg)
  }
  return res.data
}
