# CarPark / 停车场预约管理系统

CarPark is a visitor parking-booking system for a community in Guangzhou. Citizens reserve parking slots from a public H5 page without logging in and show a QR code at the gate; staff manage parking lots, bookings, and statistics from an administrative web console.

This repository is a greenfield monorepo built with Java 25, Spring Boot 4.1, Vue 3, Vite 8, and Deno.

## Architecture

```text
car-park/
├── apps/
│   ├── backend/             # REST API: Java 25 + Spring Boot 4.1 + Spring Data JPA
│   ├── web/                 # Staff admin console: Vue 3 + Element Plus + Vite on Deno
│   └── mobile/              # Public booking H5: Vue 3 + Vant 4 + Vite on Deno
├── packages/
│   ├── ui/                  # Shared Vue components, including PlateKeyboard
│   └── utils/               # Plate/phone validation, QR payloads, and formatters
├── docs/
│   ├── architecture/       # Architecture notes, concurrency design, and ADRs
│   ├── api/                 # Endpoint and response-contract documentation
│   └── agents/              # Repository contribution and issue-tracker conventions
├── scripts/                 # Development, build, test, and database-tunnel helpers
├── docker/mysql/            # MySQL client configuration for local initialization
├── .github/workflows/       # GitHub Actions CI
├── docker-compose.yml       # Local MySQL development service
├── setup.sh                 # First-time bootstrap
├── CLAUDE.md                # Detailed maintainer and Claude Code context
└── .env.example             # Environment configuration template
```

The backend exposes the REST contract on port `8080`. The web admin runs on `5173`, and the public H5 runs on `5174` under `/mobile/`. Both frontends use Deno's import map and keep `node_modules` disabled.

## Quick Start

Prerequisites: JDK 25, Deno 2.x, and optionally Docker with Compose for the local MySQL database.

```bash
git clone https://github.com/zhifeimi/car-park.git
cd car-park
./setup.sh
```

`setup.sh` checks Java and Deno, creates `.env` from `.env.example` when needed, starts the local MySQL container when Docker is available, and builds the backend. The development profile has built-in local defaults, so editing `.env` is optional for the standard local setup.

Start the applications in separate terminals:

```bash
./scripts/dev.sh backend   # REST API at http://localhost:8080
./scripts/dev.sh web       # Admin console at http://localhost:5173
./scripts/dev.sh mobile    # Public H5 at http://localhost:5174/mobile/
```

Development login: `admin` / `admin123`. This is a development fixture only; change the password before any non-development deployment.

## Configuration

Copy `.env.example` to `.env` to configure the backend, local Compose database, optional test database, or frontend API overrides. `.env` is ignored by Git and must never contain committed secrets.

| Variable | Required | Description |
|---|---:|---|
| `DB_URL` | Production | JDBC URL for the application database |
| `DB_USERNAME` | Production | Application database username |
| `DB_PASSWORD` | Production | Application database password |
| `JWT_SECRET` | Production | Random JWT signing secret of at least 32 bytes |
| `CORS_ORIGINS` | Production | Comma-separated allowed browser origins |
| `MYSQL_ROOT_PASSWORD` | Optional | Local Docker Compose root password; defaults to a development-only value |
| `MYSQL_PASSWORD` | Optional | Local Docker Compose application password; defaults to `carpark_dev` |
| `SPRING_DATASOURCE_*` | Optional | Overrides for the dev-profile datasource, including remote development tunnels |
| `TEST_JWT_SECRET` | Test only | Optional JWT secret for database-tagged integration tests |

See [.env.example](.env.example) for the complete template and comments. Production secrets must be supplied through environment variables; do not add a production YAML profile containing credentials.

## Development and Tests

```bash
./scripts/test.sh                                    # Backend tests, Deno lint, and frontend tests
./scripts/build.sh                                   # Backend JAR plus web and mobile builds
cd apps/backend && ./mvnw test                       # Default backend suite; excludes @Tag("db")
cd apps/web && deno task test                        # Web regression tests
cd packages/utils && deno task test                  # Shared utility tests
deno lint                                            # Enforced frontend lint gate
```

The `@Tag("db")` integration suite is opt-in and requires a local MySQL plus `secrets/carpark-test.env`; see [CLAUDE.md](CLAUDE.md) for the database setup and command details.

## Documentation

- [CLAUDE.md](CLAUDE.md) — project context, commands, architecture, API invariants, and known gotchas
- [Architecture documentation](docs/architecture/) — system design, concurrency, Deno/Vite resolution, and ADRs
- [API documentation](docs/api/) — endpoints, envelopes, OpenAPI notes, and anonymous-surface rules
- [CONTEXT.md](CONTEXT.md) — domain and repository context

## Using with Claude Code

This project includes [`CLAUDE.md`](CLAUDE.md), which gives Claude Code the repository context, verified commands, architecture boundaries, and security-sensitive invariants it needs to work safely in the monorepo.

```bash
claude
```

## License

MIT — see [LICENSE](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, test gates, code style, and pull-request guidance.
