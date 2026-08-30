import axios from 'axios'
import { envelopeOf } from '@car-park/utils'

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
  const body = (await promise).data
  if (envelopeOf(body).code !== 200) {
    throw new Error(envelopeOf(body).msg || '请求失败')
  }
  return body
}

/** GET /parking/park/list (unpaged — mobile lists every lot). */
export const listParks = () =>
  unwrap(http.get('/parking/park/list')).then((body) => body.rows || [])

/** GET /parking/park/leftLotNum?park=&date= → remaining lots for one lot+day (may be null). */
export const getLeftLotNum = (park, date) =>
  unwrap(http.get('/parking/park/leftLotNum', { params: { park, date } })).then(
    (body) => body.data,
  )

/** GET /parking/booking/last?cellPhone= → the caller's most recent booking (may be null).
 *  The success page loads exclusively through this endpoint — there is no anonymous
 *  per-id read (GET /parking/booking/{id} was removed with the wildcard tightening). */
export const getLastBooking = (cellPhone) =>
  unwrap(http.get('/parking/booking/last', { params: { cellPhone } })).then(
    (body) => body.data,
  )

/** POST /parking/booking — server forces status=0. */
export const addBooking = (data) => unwrap(http.post('/parking/booking', data))

/** PUT /parking/booking — partial update; cancel = { id, status: -1 }. */
export const updateBooking = (data) => unwrap(http.put('/parking/booking', data))
