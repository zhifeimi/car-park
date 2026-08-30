import { defineConfig } from 'vite'
import deno from '@deno/vite-plugin'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const vueDevtoolsShim = fileURLToPath(new URL('./src/vendor/vue-devtools-api.js', import.meta.url))
const vueShim = fileURLToPath(new URL('./src/vendor/vue.js', import.meta.url))

// The deployed H5 lives under https://park.example.com/mobile/ and talks to
// https://park.example.com/prod-api — hash router + '/mobile/' base + the
// '/prod-api' prefix keep this build drop-in compatible with that setup.
// In dev the '/prod-api' prefix is proxied to the local backend (:8080).
//
// deno() must come first: it teaches Vite/Rolldown to resolve bare
// specifiers ('vue', 'vant', …) through Deno's import map + global npm
// cache instead of a node_modules tree — that is what keeps
// `nodeModulesDir: "none"` in deno.json working.
export default defineConfig({
  base: '/mobile/',
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
      // (vant's own deps) resolve parent-relative with Node conditions —
      // the deno.json import map does not apply there — and land on
      // CJS/UMD entries the dev server then serves verbatim. A browser has
      // no module.exports/require → silent blank page. (Builds mask this
      // via Rolldown CJS interop, which is why `deno task build` was
      // always green.) These aliases rewrite every importer to the
      // package's ESM file; the deep subpath targets are then resolved
      // natively by the deno plugin. Regexes are anchored to exact ids.
      { find: /^vue$/, replacement: vueShim },
      { find: /^vue-router$/, replacement: 'vue-router/dist/vue-router.mjs' },
      { find: /^@vue\/runtime-dom$/, replacement: '@vue/runtime-dom/dist/runtime-dom.esm-bundler.js' },
      { find: /^@vue\/runtime-core$/, replacement: '@vue/runtime-core/dist/runtime-core.esm-bundler.js' },
      { find: /^@vue\/reactivity$/, replacement: '@vue/reactivity/dist/reactivity.esm-bundler.js' },
      { find: /^@vue\/shared$/, replacement: '@vue/shared/dist/shared.esm-bundler.js' },
      { find: /^@vue\/devtools-api$/, replacement: vueDevtoolsShim },
      { find: /^vant\/es\/locale\/lang\/en-US$/, replacement: 'vant/es/locale/lang/en-US.mjs' },
      { find: /^vant\/es\/locale\/lang\/zh-CN$/, replacement: 'vant/es/locale/lang/zh-CN.mjs' },
      { find: /^vant$/, replacement: 'vant/es/index.mjs' },
      // @vant/use and @vant/popperjs need no alias: their exports maps'
      // "import" condition already yields ESM. @popperjs/core has no
      // exports map (main = CJS), so force its module entry.
      { find: /^@popperjs\/core$/, replacement: '@popperjs/core/lib/index.js' },
    ],
  },
  server: {
    port: 5174,
    host: true,
    proxy: {
      '/prod-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/prod-api/, ''),
      },
    },
  },
  build: {
    outDir: 'dist',
  },
})
