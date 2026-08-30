# ADR 001: Deno Runtime and Vite Bundler with Zero Node_Modules

## Status
Accepted

## Context
Legacy frontends suffered from dependency bloat, fragile local `node_modules` states, and slow cold installations. We needed a fast, secure, modern runtime with clean native TypeScript support and zero local package directory overhead.

## Decision
Adopt **Deno 2.x** as the package and task runtime alongside **Vite 8** with `@deno/vite-plugin`.
- Enforce `nodeModulesDir: "none"`.
- Use Deno's global cache and import maps as the single source of truth for dependencies.
- Pin specific ESM bundle entries to ensure zero blank-page CJS errors in dev.

## Consequences
- Fast cold starts and zero local `node_modules` footprint.
- Single unified import map simplifies vulnerability auditing and dependency management.
- Transitive dependencies must be explicitly declared in the import map.
