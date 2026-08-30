/**
 * Envelope normalization contract — the shared detection rule for the
 * R-envelope wire format (HTTP 200 + code!==200 = business failure).
 */
import { envelopeOf } from './index.ts'

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

Deno.test('success envelope passes through with its code', () => {
  const { code, msg } = envelopeOf({ code: 200, data: 1 })
  assert(code === 200, `expected 200, got ${code}`)
  assert(msg === undefined, 'success envelope may carry no msg')
})

Deno.test('business failure keeps its non-200 code and msg', () => {
  const { code, msg } = envelopeOf({ code: 500, msg: '停车场不能为空' })
  assert(code === 500, `expected 500, got ${code}`)
  assert(msg === '停车场不能为空', 'failure msg should surface')
})

Deno.test('lenient: absent or non-numeric code normalizes to success', () => {
  const absent = envelopeOf({ data: {} })
  assert(absent.code === 200, 'absent code should normalize to 200')
  const nonNumeric = envelopeOf({ code: '401', msg: 'x' })
  assert(nonNumeric.code === 200, 'non-numeric code should normalize to 200')
})

Deno.test('TableData shape and null body both survive', () => {
  const table = envelopeOf({ total: 3, rows: [], code: 200 })
  assert(table.code === 200, 'TableData shape should be recognized')
  assert(envelopeOf(null).code === 200, 'null body should not crash')
})
