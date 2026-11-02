import viteConfig from './vite.config.js'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'

// Vitest picks test-utils' CJS node entry, whose require() cannot see Deno's npm cache.
// Resolve the same package's ESM browser file explicitly instead.
const testUtilsPackage = createRequire(import.meta.url).resolve('@vue/test-utils/package.json')
const testUtilsEsm = join(dirname(testUtilsPackage), 'dist/vue-test-utils.esm-browser.js')

export default {
  ...viteConfig,
  resolve: {
    ...viteConfig.resolve,
    alias: [
      ...(viteConfig.resolve?.alias ?? []),
      { find: /^@vue\/test-utils$/, replacement: testUtilsEsm },
    ],
  },
  test: {
    environment: 'happy-dom',
    include: ['tests/components/**/*.test.js'],
    fsModuleCache: false,
    server: { deps: { inline: [/@vue\/test-utils/] } },
  },
}
