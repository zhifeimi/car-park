// Vue Router imports this development-only hook. Its published dependency
// resolves to CJS under Deno/Vite dev mode, so use a small browser-safe bridge.
// The UI does not depend on Devtools; when a hook is present, the bridge
// forwards the standard setup event.
export function setupDevtoolsPlugin(descriptor, setup) {
  const target = typeof window === 'undefined' ? globalThis : window
  const hook = target.__VUE_DEVTOOLS_GLOBAL_HOOK__
  if (hook?.emit) {
    hook.emit('devtools-plugin:setup', descriptor, setup)
  }
}
