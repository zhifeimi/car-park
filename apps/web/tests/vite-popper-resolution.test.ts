/// <reference lib="deno.ns" />

import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'
import webConfig from '../vite.config.js'

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

Deno.test('Vite resolves Element Plus Popper imports to a browser-safe ESM module', async () => {
  const cacheDir = await Deno.makeTempDir({ prefix: 'car-park-web-vite-test-' })
  const root = fileURLToPath(new URL('../', import.meta.url))
  const entry = fileURLToPath(new URL('../src/main.js', import.meta.url))
  const server = await createServer({
    ...webConfig,
    root,
    cacheDir,
    configFile: false,
    logLevel: 'error',
    server: { ...webConfig.server, middlewareMode: true },
  })

  try {
    const elementPlusPopper = await server.pluginContainer.resolveId(
      'element-plus/es/hooks/use-popper/index.mjs',
      entry,
    )
    assert(elementPlusPopper, 'Element Plus use-popper module should resolve')

    const popper = await server.pluginContainer.resolveId('@popperjs/core', elementPlusPopper.id)
    assert(popper, 'Popper should resolve from Element Plus use-popper')
    assert(
      popper.id.endsWith('/lib/index.js'),
      `expected Popper's ESM entry, received ${popper.id}`,
    )
  } finally {
    await server.close()
    await Deno.remove(cacheDir, { recursive: true })
  }
})
