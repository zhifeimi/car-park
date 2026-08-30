import { defineConfig, searchForWorkspaceRoot } from 'vite'
import deno from '@deno/vite-plugin'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import os from 'node:os'
import path from 'node:path'

const vueDevtoolsShim = fileURLToPath(new URL('./src/vendor/vue-devtools-api.js', import.meta.url))
const vueShim = fileURLToPath(new URL('./src/vendor/vue.js', import.meta.url))
const dayjsShim = fileURLToPath(new URL('./src/vendor/dayjs.js', import.meta.url))

function getDenoCacheDir() {
  const custom = process.env.DENO_DIR
  if (custom) return custom
  const home = os.homedir()
  if (process.platform === 'darwin') return path.join(home, 'Library', 'Caches', 'deno')
  if (process.platform === 'win32') return path.join(process.env.LOCALAPPDATA || path.join(home, 'AppData', 'Local'), 'deno')
  return path.join(process.env.XDG_CACHE_HOME || path.join(home, '.cache'), 'deno')
}

// Admin console dev server: :5173, proxies /dev-api → backend :8080 (prefix stripped).
// Production builds hit /prod-api behind the same reverse proxy as the legacy deployment.
//
// deno() must come first: it teaches Vite/Rolldown to resolve bare
// specifiers ('vue', 'element-plus', …) through Deno's import map +
// global npm cache instead of a node_modules tree — that is what keeps
// `nodeModulesDir: "none"` in deno.json working.
export default defineConfig({
  plugins: [deno(), vue()],
  // Keep Vite's optimizer cache out of a directory literally named
  // node_modules — the zero-node_modules invariant stays visibly true.
  cacheDir: '.vite',
  resolve: {
    alias: [
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) },
      { find: /^@car-park\/types$/, replacement: fileURLToPath(new URL('../../packages/types/src/index.ts', import.meta.url)) },
      { find: /^@car-park\/utils$/, replacement: fileURLToPath(new URL('../../packages/utils/src/index.ts', import.meta.url)) },
      { find: /^@car-park\/ui$/, replacement: fileURLToPath(new URL('../../packages/ui/src/index.ts', import.meta.url)) },
      { find: /^@car-park\/api-client$/, replacement: fileURLToPath(new URL('../../packages/api-client/src/index.ts', import.meta.url)) },

      // Dev-mode blank-page fix. Imports made INSIDE npm-cache packages
      // (element-plus's own deps) resolve parent-relative with Node
      // conditions — the deno.json import map does not apply there — and
      // land on CJS/UMD entries the dev server then serves verbatim. A
      // browser has no module.exports/require → silent blank page. (Builds
      // mask this via Rolldown CJS interop, which is why `deno task build`
      // was always green.) These aliases rewrite every importer to the
      // package's ESM file; the deep subpath targets are then resolved
      // natively by the deno plugin (all these packages allow ./dist/* or
      // have no exports map). Regexes are anchored so 'dayjs' does not
      // also swallow 'dayjs/plugin/…'.
      { find: /^vue$/, replacement: vueShim },
      { find: /^vue-router$/, replacement: 'vue-router/dist/vue-router.mjs' },
      { find: /^@vue\/runtime-dom$/, replacement: '@vue/runtime-dom/dist/runtime-dom.esm-bundler.js' },
      { find: /^@vue\/runtime-core$/, replacement: '@vue/runtime-core/dist/runtime-core.esm-bundler.js' },
      { find: /^@vue\/reactivity$/, replacement: '@vue/reactivity/dist/reactivity.esm-bundler.js' },
      { find: /^@vue\/shared$/, replacement: '@vue/shared/dist/shared.esm-bundler.js' },
      { find: /^@vue\/devtools-api$/, replacement: vueDevtoolsShim },
      // @popperjs/core has no exports map; its main entry is CommonJS.
      { find: /^@popperjs\/core$/, replacement: '@popperjs/core/lib/index.js' },
      { find: /^async-validator$/, replacement: 'async-validator/dist-web/index.js' },
      { find: /^dayjs$/, replacement: dayjsShim },
      { find: /^dayjs\/plugin\/(.+?)(\.js)?$/, replacement: 'dayjs/esm/plugin/$1/index.js' },
      { find: /^dayjs\/locale\/(.+?)(\.js)?$/, replacement: 'dayjs/esm/locale/$1.js' },
      { find: /^memoize-one$/, replacement: 'memoize-one/dist/memoize-one.esm.js' },
      { find: /^@ctrl\/tinycolor$/, replacement: '@ctrl/tinycolor/dist/module/public_api.js' },
      { find: /^normalize-wheel-es$/, replacement: 'normalize-wheel-es/dist/index.mjs' },
    ],
  },
  server: {
    port: 5173,
    host: true,
    fs: {
      allow: [
        searchForWorkspaceRoot(process.cwd()),
        getDenoCacheDir(),
      ],
    },
    proxy: {
      '/dev-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/dev-api/, ''),
      },
    },
  },
  build: {
    outDir: 'dist',
  },
})
