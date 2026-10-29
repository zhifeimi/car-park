import axios from 'axios'
import { envelopeOf, localizeServerMessage } from '@car-park/utils'
import { getLocale } from '@/locales'

// '/prod-api' mirrors the deployed H5 (park.example.com/prod-api). In dev the
// Vite proxy rewrites it to http://localhost:8080; in prod the reverse proxy
// serves it. No origin is ever hardcoded here (legacy main.js did).
const http = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/prod-api',
  timeout: 15000,
})

/**
 * Envelope contract detection lives in @car-park/utils (envelopeOf) — shared
 * with the admin console. This adapter only decides what to do: throw on
 * business failure, hand the full envelope body back on success.
 */
async function unwrap(promise) {
  let body
  try {
    body = (await promise).data
  } catch (err) {
    // HTTP-level failures (429 rate limit) still carry the server's {msg}
    throw new Error(localizeServerMessage(err.response?.data?.msg, getLocale()) || err.message)
  }
  if (envelopeOf(body).code !== 200) {
    // empty message → the view shows its own localized fallback
    throw new Error(localizeServerMessage(envelopeOf(body).msg, getLocale()) ?? '')
  }
  return body
}

/** GET /parking/park/list (unpaged — mobile lists every lot) → { rows, today }.
 *  today = the park's date (yyyy-MM-dd) by the server's park clock; null on an older server. */
export const listParks = () =>
  unwrap(http.get('/parking/park/list')).then((body) => ({
    rows: body.rows || [],
    today: body.today || null,
  }))

/** GET /parking/park/leftLotNum?park=&date= → remaining lots for one lot+day (may be null). */
export const getLeftLotNum = (park, date) =>
  unwrap(http.get('/parking/park/leftLotNum', { params: { park, date } })).then(
    (body) => body.data,
  )

/** GET /parking/booking/last?cellPhone=&carPlate= → the caller's most recent booking for
 *  that phone + plate (may be null). The success page loads exclusively through this
 *  endpoint — there is no anonymous per-id read (GET /parking/booking/{id} was removed
 *  with the wildcard tightening). */
export const getLastBooking = (cellPhone, carPlate) =>
  unwrap(http.get('/parking/booking/last', { params: { cellPhone, carPlate } })).then(
    (body) => body.data,
  )

/** POST /parking/booking — server forces status=0. */
export const addBooking = (data) => unwrap(http.post('/parking/booking', data))

/** PUT /parking/booking — cancel only: { id, cellPhone, status: -1 } (phone must match). */
export const updateBooking = (data) => unwrap(http.put('/parking/booking', data))
