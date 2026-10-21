# CLAUDE.md
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is
A visitor parking-booking system (停车场预约管理系统) for the Huangma/Lingtou communities of Changling Street, Guangzhou. Citizens book slots from a public H5 page (no login) and show a QR at the gate; staff run an admin console. This is a **greenfield rewrite** of a legacy Java 8 / Spring Boot 2.5 system — none of the old framework/JSON/monitor/paging/Redis/uni-app code survives.
**Monorepo layout**, structured into `apps/`, `packages/`, `docs/`, and `scripts/`:
| Path | Role | Stack |
|---|---|---|
| `apps/backend/` | REST backend | Java 25 (LTS), Spring Boot **4.1** (Framework 7, Hibernate 7, Jackson 3), Spring Data JPA, Spring Security + stateless JWT, MySQL, HikariCP — packages `press.mizhifei.parking.*` |
| `apps/web/` | Admin console | Vue 3 + Element Plus + Vite 8 on **Deno** — zero `node_modules` |
| `apps/mobile/` | Public booking H5 SPA | Vue 3 + Vant 4 + qrcode + Vite 8 on **Deno** — zero `node_modules` |
| `packages/utils/` | Shared Utils | Plate regex, phone validation, QR generator (`QrPayload`), formatters |
| `packages/ui/` | Shared UI | Reusable Vue components (`PlateKeyboard`) |
Plus root `docker-compose.yml` (dev MySQL only) and `apps/backend/src/main/resources/db/{schema,data}.sql`.

## Commands
```bash
# Developer runner scripts
./scripts/dev.sh backend                             # :8080, dev profile by default
./scripts/dev.sh web                                 # :5173, proxies /dev-api → :8080
./scripts/dev.sh mobile                              # :5174, serves /mobile/, proxies /prod-api → :8080
./scripts/build.sh                                   # Builds backend JAR + web dist + mobile dist
./scripts/test.sh                                    # Runs backend tests + Deno lint
# Direct commands
cd apps/backend && ./mvnw spring-boot:run            # :8080, dev profile by default; wrapper downloads Maven
cd apps/backend && ./mvnw clean package              # → target/car-park-server-*.jar
cd apps/backend && ./mvnw test                       # unit + context smoke; @Tag("db") excluded
cd apps/backend && ./mvnw test -Dsurefire.excludedGroups=   # + real-MySQL integration suite (needs secrets/carpark-test.env)
# Local MySQL (Docker)
docker compose up -d                                 # mysql:9.7, seeds from apps/backend/.../db/ on first volume creation
# Remote dev MySQL (dev-db.example.com — Docker mysql:9.7, loopback-only publish,
# carpark account REQUIRE SSL, seeds from the same db/ scripts; compose project /opt/example-carpark-mysql)
./scripts/db-tunnel.sh                               # tunnel — the ONLY way in (needs local :3306 free)
cd apps/backend && SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/carpark?...&useSSL=true&requireSSL=true&verifyServerCertificate=false' \
  SPRING_DATASOURCE_USERNAME=carpark SPRING_DATASOURCE_PASSWORD='<server ~/.config/carpark/carpark.env>' \
  ./mvnw spring-boot:run
# Frontends (Deno tasks wrap Vite; never npm/yarn/pnpm here)
cd apps/web    && deno task dev                      # :5173, proxies /dev-api → :8080 (prefix stripped)
cd apps/mobile && deno task dev                      # :5174, serves /mobile/, proxies /prod-api → :8080 (prefix stripped)
deno task build                                      # → dist/ (both)
deno lint                                            # root / both
```
Dev login: **admin / admin123** (seed in `data.sql`). Health: `GET /actuator/health`. Swagger (dev profile only): `/swagger-ui.html`.

## Architecture


### Server (`apps/backend/`, single Maven module)
`CarParkApplication` (`@EnableJpaAuditing`) → `config/` (CORS from `app.cors.allowed-origins`, JPA auditing with `AuditorAware` = JWT username or `"system"`, OpenAPI `@Profile("dev")`) · `security/` (SecurityConfig, JwtService/JwtAuthenticationFilter, RateLimitFilter — bucket4j per client IP = `getRemoteAddr()`, which `forward-headers-strategy: native` (Tomcat RemoteIpValve) resolves from XFF past trusted proxies only; never read the XFF header directly — the first hop is client-controlled) · `domain/` (BaseEntity audit `@MappedSuperclass`; `Park`, `ParkingBooking`, `User`) · `repository/` (native SQL for availability + statistics, `JpaSpecificationExecutor` for booking filters) · `service/` · `web/` (controllers, `envelope/R` + `TableData`, `dto/`). **No Redis** — JWT is stateless (720 min); rate limiting is in-memory, single instance only.
Tables (existing live schema, `ddl-auto: none`): `cp_park`, `cp_parking_booking`, `cp_user` (new — admin users only; no RBAC/menus/roles by design).


### API contract (frozen — the deployed H5 at park.example.com depends on it)
Envelopes: `R` = `{code,msg,data}`; `TableData` = `{total,rows,code,msg}` with `pageNum`/`pageSize`. Public endpoints return **HTTP 200 + envelope** even on business failure (`code:500`); only 401/403/429 use real HTTP statuses.
**Anonymous surface — exactly these six, nothing else** (`permitAll` in `SecurityConfig`; a new endpoint is not public until added there): `POST /auth/login` (5/min/IP); `GET /parking/park/list`; `GET /parking/park/leftLotNum`; `GET /parking/booking/last?cellPhone=&carPlate=` (both required); `POST /parking/booking`; `PUT /parking/booking` (the last three 10/min/IP each). Deliberate hardening vs legacy: wildcard GETs (`statisticInfo`, `GET /{id}`) and `PUT /parking/park` are now auth-required — `GET /parking/booking/{id}` was then deleted outright (zero callers; the mobile success page loads via `/last`). `GET /parking/booking/list` is admin-only (2026-10-21): unfiltered it dumped every plate and phone, and the H5 never called it. The H5 and backend must ship together — the success page now stores the plate too and sends it to `/last`. The boundary is pinned by `AnonymousSurfaceMatrixTest` (default suite, no DB): it enumerates the live route registry, so adding an endpoint or a `permitAll` entry fails the test until its `EXPECTED_PUBLIC` oracle set is updated in the same commit.
- **`left_lot_num` is never stored.** List and `leftLotNum` compute `lot_num − count(bookings status=0 for park+date)` in native SQL; it frees itself on cancel. Do not persist it.
- **Park↔booking join key is the lot `name` string**, not id (live-schema constraint). Renaming a park orphans its bookings from availability counts — admin UI warns; don't change the column without a data migration.
- Booking filters are **equality only** (name LIKE is the sole LIKE); fixed sort `booking_date DESC, create_time DESC, cell_phone, create_by, car_plate`; `/last` = latest by `create_time` per phone **+ plate** (phone alone let anyone walk number ranges).
- `statisticInfo?date=` keeps its **String** param (`yyyy-MM-dd HH:mm:ss`) and four native queries verbatim (day / week Monday-start via `WEEKDAY` / month 1st→`LAST_DAY` / year) → `{today,week,month,year}Booking: [{park,booking}]`.
- Cancel = `PUT /parking/booking` carrying `{id, cellPhone, status:-1}` — the ONLY mutation the public surface accepts; the phone must match the booking (ids are sequential), and a mismatch answers `预约记录不存在` exactly like a missing id. Anything but `status:-1` is rejected with `不支持修改预约记录内容` (legacy `<if>` partial-update semantics survived only in the DTO shape, not the behaviour).
- **Intake concurrency mechanism (2026-08-17, proven by the `@Tag("db")` suite)**: `BookingService.intake` runs at **READ COMMITTED** — under MySQL's default REPEATABLE READ the MVCC snapshot pins at the first read, so the checks under the park lock would never see a concurrent intake's committed row and overbook. Vehicle exclusivity is checked twice: fail-fast before the lock, authoritative under the `PESSIMISTIC_WRITE` park lock (per-park serialization; a microsecond cross-park window remains). A UNIQUE index on `cp_parking_booking(car_plate, booking_date, status)` would close it but touches the live schema — do not add without sign-off.
- **Staff QR contract**: mobile success page QR payload = `JSON.stringify({车牌号,手机号,停车场,日期})` — gate scanners parse those exact keys.
- Validation (worldwide since 2026-10-15): the server (`ParkingDtos.PLATE_REGEX`/`PHONE_REGEX`) accepts any country's plate — letters of any script + digits, single inner space/hyphen/middle dot, 2–16 chars; the character class is the XSS guard — and a mainland mobile or E.164 number. The H5's `VITE_REGION` picks client strictness: `cn` (default, the deployed H5) keeps the province keyboard + strict CN plate (`[A-HJ-NP-Z]`, no I/O) and `^1[3-9]\d{9}$`; `intl` normalizes (`normalizePlate`/`normalizePhone`) then checks `PLATE_REGEX`/`E164_REGEX`. The same samples are pinned in `ParkingDtosTest` and `packages/utils/src/validation.test.ts` — keep both in sync. Columns are `VARCHAR(20)`, so no schema change was needed.
- **No request-body sanitizing filter (ADR-005)**: what the client submits binds verbatim; XSS defence is DTO `@Pattern`/`@Size` validation + Vue output escaping. `RequestBodyIntegrityTest` (default suite) pins this — a body-rewriting filter would fail it. Legacy rows may carry `&lt;` mangling; `scripts/ops/xss-remediation.sql` audits.


### Frontends — Deno resolution model (the critical gotcha)
`deno.json`'s import map is the **single dependency declaration**; `nodeModulesDir: "none"` enforces zero `node_modules`. `@deno/vite-plugin` must stay **first** in `vite.config.js` plugins — it makes Vite/Rolldown resolve bare specifiers through the import map + Deno's global cache. **Transitive npm deps are not auto-resolved**: every bare specifier a bundled library imports at runtime needs an import-map entry, with the range copied from that library's own `package.json` (so exactly one copy bundles). Build failures name the missing package explicitly — add the entry.
**Dev serves npm files verbatim — CJS entries blank the page.** Builds tolerate CJS via Rolldown interop, but in dev the browser executes files as-is: any package resolving to its Node/CJS entry (`main` field, `node` export condition) crashes silently (`module.exports`/`require` undefined → blank page). Two layers fix this, both required:
- **Import-map pins** (cover project-source imports and the whole build graph): pin the package's ESM file — `"vue": "npm:vue@^3.5.0/dist/vue.runtime.esm-bundler.js"`. **Caveat**: a subpath pin makes Deno treat the key as a prefix (resolving `pkg/deep/path` → pinned-path + `/deep/path`, a broken doubled path) — packages whose own files are deep-imported need **root pins** (`@vant/use`, `@popperjs/core` in mobile).
- **`resolve.alias` regexes in `vite.config.js`** (cover imports made *inside* npm-cache packages, where the import map does not apply): `{find: /^dayjs$/, replacement: 'dayjs/esm/index.js'}`. The alias target is a deep subpath that the plugin then resolves natively — it must be allowed by the package's exports map (vue's `./dist/*` ✓, async-validator/dayjs have no exports map ✓). Packages with a proper `"import"` exports condition (`@vant/use`, `@vue/devtools-api`) need **no alias at all**.
`qrcode` ships CJS only (even its browser entry), so it's **pre-bundled to ESM at `apps/mobile/src/vendor/qrcode.js`** (esbuild one-off; regeneration command in the file header) and the import map points at that local file. Element Plus and Vant are registered globally (fewer plugins under Deno) — the build's chunk-size warnings are expected. `deno lint` is the enforced gate; the code style is deliberately semicolon-light, so `deno fmt --check` reports diffs — do not bulk-reformat.
**Design system (Trailhead)**: every color, type size and spacing value comes from `packages/ui/src/tokens.css` (`--np-*`), mapped onto Vant in `apps/mobile/src/styles/index.css` and onto Element Plus + the `--cp-*` names in `apps/web/src/styles/index.css`. No hex literals in views, and no `color-mix()` in the public H5 (old WeChat WebViews). Per-park setup is build-time only: `VITE_REGION` (`cn`|`intl`), `VITE_SITE_NAME`, `VITE_SITE_NAME_EN`, `VITE_PARK_THEME` (`forest`|`canyon`|`glacier`|`coast`). Rationale is in `packages/ui/DESIGN.md`; `packages/ui/preview.html` is the live specimen.
Mobile parity with the deployed H5: hash router + `base: '/mobile/'` + `/prod-api` axios prefix — `mobile/dist` is a drop-in for the existing nginx setup. Admin dev uses `/dev-api` (stripped by the Vite proxy); prod builds use `/prod-api` behind a reverse proxy.

## Critical gotchas
- **Secrets: env vars only, no YAML secrets, ever.** Base `application.yml` has `${DB_URL}`/`${DB_USERNAME}`/`${DB_PASSWORD}`/`${JWT_SECRET}`/`${CORS_ORIGINS}` with **no defaults — prod fails fast** if missing. Dev defaults (localhost DB, dev-only JWT literal) live **only** in `application-dev.yml`. There is no `application-prod.yml` and must never be one.
- **Dev never touches any production database.** Legacy deployment configuration is excluded from this release; the rewrite's only production runbook is: create the `cp_user` table + one seeded admin with a **changed** password against an existing database — never run full `schema.sql`/`data.sql` there, never put production addresses in any config. The acceptance check for forbidden legacy content must return zero hits outside excluded legacy documents; do not paste such patterns into source or docs, or the check self-matches.
- **Remote dev DB override needs relaxed binding**: `application-dev.yml` holds literal localhost values, so `$DB_URL`-style env placeholders do **not** override them — only `SPRING_DATASOURCE_URL`/`SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` env vars do (verified: Hikari logs the effective JDBC URL). The remote `carpark` account is `REQUIRE SSL` — JDBC needs `useSSL=true&requireSSL=true`, CLI needs `--ssl-mode=REQUIRED`. Seed scripts must load with an explicit client charset (`--default-character-set=utf8mb4`) — a POSIX-locale mysql client negotiates latin1 and double-encodes CJK (symptom: `CHAR_LENGTH(name)` 15 instead of 5, park-name lookups return null).
- **Boot 4 / Hibernate 7 / Jackson 3 breaks already solved — don't revert**: dialect pinned explicitly (`hibernate.dialect: org.hibernate.dialect.MySQLDialect`, Hibernate 7 no longer guesses from JDBC URL); `WRITE_DATES_AS_TIMESTAMPS` moved to `DateTimeFeature` → property is `spring.jackson.datatype.datetime.write-dates-as-timestamps`; auto-configured mapper is `tools.jackson.databind.ObjectMapper` (`com.fasterxml` = annotations only); MockMvc/WebMvcTest moved out of `spring-boot-starter-test` into **`spring-boot-webmvc-test`** (package `org.springframework.boot.webmvc.test.autoconfigure` — `@AutoConfigureMockMvc` lives there). When in doubt, read ground truth from the jars: `unzip -p ~/.m2/repository/.../spring-boot-jackson-4.1.0.jar META-INF/spring-configuration-metadata.json`.
- **There is no VCS history in this release**; do not rely on `git log`/`git diff` for provenance.
- Admin menus/routes are **static** (no DB-driven `sys_menu`); a single implicit admin role (no `@PreAuthorize`/permission strings).
- UI copy lives in each app's `locales/` (en, zh-CN); first visit follows the browser language, `<html lang>` tracks the choice. Server messages stay Chinese (frozen contract) and are translated client-side by `localizeServerMessage` (`packages/utils/src/server-messages.ts`); `server-messages.test.ts` scans the Java sources and fails on an untranslated new message. The park's clock is the backend process `TZ` (default Australia/Sydney), read once into the `parkClock` bean (`JpaAuditingConfig`) — inject `Clock` and use `LocalDate.now(clock)`; never bare `LocalDate.now()` (that is the host zone) or SQL `curdate()` (the DB zone). Booking status: `0` = 预约成功, `-1` = 已取消.

## Agent skills


### Issue tracker
- Legacy issue notes, when used, live under `.scratch/` and are intentionally excluded from the open-source copy.


### Triage labels
Default label vocabulary (five canonical roles, unchanged). See `docs/agents/triage-labels.md`.


### Domain docs
Single-context layout — one `CONTEXT.md` + `docs/architecture/adr/` at the repo root.
