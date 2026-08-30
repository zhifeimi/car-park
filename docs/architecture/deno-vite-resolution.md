# Deno + Vite Zero-Node_Modules Resolution Model

## Core Invariant

The frontend applications (`apps/web` and `apps/mobile`) run directly on **Deno 2.x** with **zero `node_modules`** (`nodeModulesDir: "none"`). All npm dependencies are resolved through Deno's global cache and import maps.

## Key Resolution Mechanisms

### 1. `@deno/vite-plugin` as First Plugin
`@deno/vite-plugin` must remain the first plugin in `vite.config.js`. It teaches Vite and Rolldown to resolve bare module specifiers through Deno's import map and global npm cache.

### 2. Transitive Dependency Import Map Entries
Because there is no local `node_modules` directory, Vite and Rolldown cannot perform filesystem traversal to find transitive dependencies. Every bare specifier that a library imports must be present in the import map with versions aligned to that library's `package.json`.

### 3. ESM Pinning & Aliases
In development mode, Deno serves npm files directly. Some packages default to CommonJS/UMD when resolved with standard Node conditions, which causes blank pages in browser execution.

Two layers solve this:
1. **Import Map Pins**: Pin the package specifier to its ESM bundle entry:
   ```json
   "element-plus": "npm:element-plus@^2.14.4",
   "vue-runtime": "npm:vue@3.5.41/dist/vue.runtime.esm-bundler.js"
   ```
2. **`resolve.alias` in `vite.config.js`**: Rewrites nested subpath imports within dependencies to their ESM equivalents:
   ```javascript
   { find: /^dayjs$/, replacement: 'dayjs/esm/index.js' }
   ```

### 4. Monorepo Shared Packages
Local packages (`@car-park/utils`, `@car-park/ui`) are resolved both via:
- Import map entries in root `deno.json` and member `deno.json` files.
- `resolve.alias` entries in `vite.config.js` pointing to TypeScript sources.
