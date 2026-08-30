/**
 * Parking domain client — parks and bookings. Every function resolves to
 * domain data ({rows,total} for paged lists, objects elsewhere), resolves
 * void for mutations, and rejects with Error(server msg) on business failure;
 * UI side effects (toasts, re-login dialog) live in http.js's implementation.
 */
import { download, request } from './http.js'
import { formatDateTime } from '@car-park/utils'

// ---- parks ----------------------------------------------------------

/** GET /parking/park/list — {rows,total}; filters pass to the wire verbatim. */
export const listPark = (filters) => request({ url: '/parking/park/list', method: 'get', params: filters })

/** GET /parking/park/{id} — the park object. */
export const getPark = (id) => request({ url: `/parking/park/${id}`, method: 'get' })

/** POST /parking/park — void. */
export const addPark = (data) => request({ url: '/parking/park', method: 'post', data })

/** PUT /parking/park — void. */
export const updatePark = (data) => request({ url: '/parking/park', method: 'put', data })

/** DELETE /parking/park/{ids} — void; comma-separated ids per the legacy wire. */
export const deletePark = (ids) => request({ url: `/parking/park/${ids}`, method: 'delete' })

/**
 * POST /parking/park/export — resolves the .xlsx workbook Blob.
 * Filters go on the query string (@RequestParam on the server).
 */
export const exportParks = (filters) => download('/parking/park/export', filters)

// ---- bookings ---------------------------------------------------------

/** GET /parking/booking/list — {rows,total}. */
export const listBooking = (filters) => request({ url: '/parking/booking/list', method: 'get', params: filters })

/** DELETE /parking/booking/{ids} — void. */
export const deleteBooking = (ids) => request({ url: `/parking/booking/${ids}`, method: 'delete' })

/**
 * GET /parking/booking/statisticInfo — dashboard aggregate.
 * The client owns the wire quirk that `date` must arrive as
 * 'yyyy-MM-dd HH:mm:ss' (the server SQL parses it with date_format):
 * pass a Date, or nothing for "now".
 */
export const getStatisticInfo = (date) =>
  request({
    url: '/parking/booking/statisticInfo',
    method: 'get',
    params: { date: formatDateTime(date ?? new Date(), true) },
  })

/**
 * POST /parking/booking/export — resolves the .xlsx workbook Blob.
 */
export const exportBookings = (filters) => download('/parking/booking/export', filters)
