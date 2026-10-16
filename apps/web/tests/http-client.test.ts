/**
 * HTTP client seam contract — tests cross the same interface views do:
 * list* → {rows,total}; domain getters → data; mutations → void;
 * any business failure → thrown Error(server msg); 401 → single-flight dialog;
 * exports → workbook Blob handed back for saving, JSON error payloads
 * surfaced as thrown Errors.
 *
 * Transport is injected through axios's adapter slot; UI side effects are
 * observed by temporarily replacing the `ui` hooks. No view code involved.
 */
import { assert, assertEquals, assertRejects } from '@std/assert'
import { useUserStore } from '../src/stores/user.js'
import { pinia } from '../src/stores/pinia.js'
import { http, ui } from '../src/api/http.js'
import { deleteUser } from '../src/api/auth.js'
import { getLocale } from '../src/locales/index.js'
import { localizeServerMessage } from '@car-park/utils'
import {
  addPark,
  deleteBooking,
  exportBookings,
  getPark,
  getStatisticInfo,
  listBooking,
  listPark,
  updatePark,
} from '../src/api/parking.js'

/** Install a fake transport that answers every request with `respond(config)`.
 *  Mimics axios: the resolved response carries the request's config. */
function serve(respond: (config: never) => unknown): unknown[] {
  const requests: unknown[] = []
  http.defaults.adapter = ((config: Record<string, unknown>) => {
    requests.push(config)
    const response = respond(config as never) as Record<string, unknown>
    return Promise.resolve({ ...response, config })
  }) as never
  return requests
}

function req(requests: unknown[], i: number): { params?: Record<string, unknown>; headers?: Record<string, string> } {
  return requests[i] as { params?: Record<string, unknown>; headers?: Record<string, string> }
}

function ok(data: unknown, extra: { headers?: Record<string, string> } = {}) {
  return {
    data,
    status: 200,
    statusText: 'OK',
    headers: extra.headers ?? {},
    config: {},
  }
}

function withUi(overrides: Record<string, unknown>, fn: () => unknown): Promise<unknown> {
  const original = { ...ui } as Record<string, unknown>
  Object.assign(ui, overrides)
  return Promise.resolve().then(fn).finally(() => {
    const bag = ui as unknown as Record<string, unknown>
    Object.keys(original).forEach((k) => {
      bag[k] = original[k]
    })
  })
}

Deno.test('listBooking resolves {rows,total} from the TableData envelope', async () => {
  const requests = serve(() =>
    ok({ total: 2, rows: [{ id: 1 }, { id: 2 }], code: 200, msg: '查询成功' }))
  const result = await listBooking({ carPlate: '粤A12345', pageNum: 1, pageSize: 10 })
  assertEquals(result, { rows: [{ id: 1 }, { id: 2 }], total: 2 })
  assertEquals(req(requests, 0).params, { carPlate: '粤A12345', pageNum: 1, pageSize: 10 })
})

Deno.test('listPark sends filters verbatim and resolves unpaged rows with their count', async () => {
  const rows = [{ id: 7, name: '岭头停车场' }]
  serve(() => ok({ total: 1, rows, code: 200, msg: '查询成功' }))
  const result = await listPark({ name: '岭头' })
  assertEquals(result, { rows, total: 1 })
})

Deno.test('business failure (code 500) rejects with the server message and toasts once', async () => {
  const toasts: string[] = []
  await withUi({ message: (m: string) => toasts.push(m) }, async () => {
    serve(() => ok({ code: 500, msg: '该停车场当日预约名额已满', data: null }))
    const err = await assertRejects(() => addPark({ name: 'x', lotNum: 3 }))
    // shown in the UI's language: Chinese as sent for zh-CN, English otherwise
    const shown = localizeServerMessage('该停车场当日预约名额已满', getLocale())
    assertEquals((err as Error).message, shown)
    assertEquals(toasts, [shown])
  })
})

Deno.test('concurrent 401s open the re-login dialog exactly once', async () => {
  let dialogs = 0
  await withUi({
    reloginDialog: () => {
      dialogs++
      return Promise.resolve()
    },
  }, async () => {
    serve(() => ok({ code: 401, msg: '认证失败，无法访问系统资源', data: null }))
    const [a, b] = await Promise.allSettled([listBooking({}), listBooking({})])
    assertEquals(a.status, 'rejected')
    assertEquals(b.status, 'rejected')
    assertEquals(dialogs, 1)
  })
})

Deno.test('getPark resolves the domain object behind the envelope', async () => {
  serve(() => ok({ code: 200, msg: '操作成功', data: { id: 1, name: '黄麻停车场' } }))
  const park = await getPark(1)
  assertEquals(park, { id: 1, name: '黄麻停车场' })
})

Deno.test('mutations resolve void on success', async () => {
  serve(() => ok({ code: 200, msg: '操作成功', data: null }))
  assertEquals(await updatePark({ id: 1 }), undefined)
  assertEquals(await deleteBooking('9'), undefined)
  assertEquals(await deleteUser(5), undefined)
})

Deno.test('getStatisticInfo owns the wire date format (yyyy-MM-dd HH:mm:ss)', async () => {
  const requests = serve(() => ok({ code: 200, msg: '操作成功', data: {} }))
  await getStatisticInfo(new Date(2026, 7, 24, 10, 0, 0))
  assertEquals(req(requests, 0).params?.date, '2026-08-24 10:00:00')
})

Deno.test('exportBookings returns the workbook blob and sends the bearer token', async () => {
  useUserStore(pinia).token = 'tok123'
  const requests = serve(() =>
    ok(new Blob(['workbook']), {
      headers: { 'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
    }))
  const blob = await exportBookings({ park: '岭头停车场' })
  assert(blob instanceof Blob)
  assertEquals(req(requests, 0).headers?.Authorization, 'Bearer tok123')
})

Deno.test('exportBookings surfaces a JSON error payload as a thrown Error, not a saved file', async () => {
  serve(() =>
    ok(new Blob([JSON.stringify({ code: 500, msg: '导出失败' })]), {
      headers: { 'content-type': 'application/json' },
    }))
  const err = await assertRejects(() => exportBookings({}))
  assertEquals((err as Error).message, '导出失败')
})
