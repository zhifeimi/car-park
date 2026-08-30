# Contributing to CarPark

Thank you for contributing to CarPark / 停车场预约管理系统. Keep changes focused, documented, and compatible with the frozen public booking contract.

## Development setup

Run the repository bootstrap from the project root:

```bash
./setup.sh
```

It checks JDK 25 and Deno, starts the optional local MySQL Compose service when Docker is available, and builds the backend. Use the commands in [CLAUDE.md](CLAUDE.md) for the architecture, service boundaries, database notes, and frontend Deno resolution rules.

## Test and lint gates

Run the default backend suite and frontend checks before opening a pull request:

```bash
cd apps/backend && ./mvnw test
cd ../.. && deno lint
```

The database integration suite is deliberately opt-in. It uses the tests tagged `@Tag("db")`, a real local MySQL instance, and credentials in `secrets/carpark-test.env`. Do not commit that file or any populated environment file. In this environment, the two `RequestBodyIntegrityTest` cases may also require local MySQL on port `3306` even though they are not database-tagged.

```bash
cd apps/backend
./mvnw test -Dsurefire.excludedGroups=
```

The repository enforces `deno lint`. Frontend style is deliberately semicolon-light; do not run a bulk `deno fmt` reformat as part of an unrelated change. Preserve existing formatting unless a change requires a narrowly scoped adjustment.

## API compatibility rule

The anonymous API surface is intentionally limited and covered by `AnonymousSurfaceMatrixTest`. A new public endpoint must be added to the `SecurityConfig` `permitAll` rules **and** to the test's `EXPECTED_PUBLIC` oracle in the same commit. Do not make an endpoint public by accident, and update the API documentation when the frozen contract changes.

## Code style and scope

- Keep the community-facing UI language and existing Chinese domain terms consistent.
- Keep validation rules synchronized between the backend DTOs and `packages/utils`.
- Use environment variables for secrets; never add credentials to YAML, source, tests, or documentation.
- Respect the Deno `nodeModulesDir: "none"` setup. Do not introduce `node_modules`, npm lockfiles, or a second frontend package-management workflow.
- Read the relevant architecture and ADR documentation before changing concurrency, security, database schema, or API-envelope behavior.

## Issues and pull requests

For a bug report, include the affected app or endpoint, reproduction steps, expected and actual behavior, environment details, and relevant logs with secrets removed. For a feature request, explain the user problem, proposed behavior, and API or data-model impact.

Create a focused branch and keep each pull request reviewable. Describe the motivation, implementation, tests run, database requirements, and any contract or migration impact. Update documentation and tests in the same pull request when behavior changes. Do not commit generated `dist/` output, local logs, `.env` files, or test secrets.

Before requesting review, run the applicable backend tests, `deno lint`, and frontend tests or builds. A maintainer may request the `@Tag("db")` suite for changes involving persistence or booking concurrency.

## Using Claude Code

Claude Code reads [CLAUDE.md](CLAUDE.md) automatically. Keep that file accurate when verified commands, architecture boundaries, or important operational gotchas change; do not use it to store secrets or private deployment details.
