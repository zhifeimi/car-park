/// <reference lib="deno.ns" />

import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'
import webConfig from '../vite.config.js'

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

Deno.test('Vite allows serving Dayjs ESM plugins from Deno npm cache without 403 Forbidden', async () => {
  const cacheDir = await Deno.makeTempDir({ prefix: 'car-park-web-vite-dayjs-test-' })
  const root = fileURLToPath(new URL('../', import.meta.url))
  const entry = fileURLToPath(new URL('../src/main.js', import.meta.url))
  const server = await createServer({
    ...webConfig,
    root,
    cacheDir,
    configFile: false,
    logLevel: 'error',
    server: { ...webConfig.server, port: 0 },
  })

  try {
    await server.listen()
    const address = server.httpServer?.address()
    assert(address && typeof address === 'object', 'Server should be listening with address object')
    const port = address.port

    const datePickerPanel = await server.pluginContainer.resolveId(
      'element-plus/es/components/date-picker-panel/src/date-picker-panel.mjs',
      entry,
    )
    assert(datePickerPanel, 'Element Plus date-picker-panel module should resolve')

    const localeData = await server.pluginContainer.resolveId('dayjs/plugin/localeData.js', datePickerPanel.id)
    assert(localeData, 'Dayjs localeData plugin should resolve')
    assert(localeData.id.includes('esm/plugin/localeData'), `expected ESM localeData, got ${localeData.id}`)

    const res = await fetch(`http://127.0.0.1:${port}/@fs${localeData.id}`)
    assert(
      res.status === 200,
      `Expected status 200 for dayjs plugin in Deno cache, received ${res.status} (outside Vite serving allow list?)`,
    )
  } finally {
    await server.close()
    await Deno.remove(cacheDir, { recursive: true })
  }
})
