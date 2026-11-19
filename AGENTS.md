# AGENTS.md

SaludGrau: appointment system for Centro de Salud Miguel Grau. `citas_salud/` is a Spring Boot REST API, `frontend/` a Next.js App Router site, `sql/` the PostgreSQL 17 scripts. `frontend/README.md` is the real setup guide and endpoint table; `docs/DESPLIEGUE.md` covers Docker deployment and backups.

## Commands

DB and installer commands run from the repo root, backend from `citas_salud/`, frontend from `frontend/`.

- DB install: `powershell -ExecutionPolicy Bypass -File sql\instalar_bd.ps1` (`-Actualizar` idempotent, `-Reiniciar` drops and recreates). The installer auto-loads every `sql/NN_*.sql` from `06` upward, so new scripts must be re-runnable (`CREATE TABLE IF NOT EXISTS`, `ON CONFLICT DO NOTHING`) or `-Actualizar` fails on existing data.
- Backend: `.\mvnw spring-boot:run` on 8080, JDK 21.
- Tests: `.\mvnw test` runs unit tests only. `@Tag("integracion")` tests such as `CitasSaludApplicationTests` need PostgreSQL and are excluded through `excludedGroups`; run them with `.\mvnw test -DexcludedGroups= -Dtest=CitasSaludApplicationTests`, or one method with `-Dtest=CitaServiceTest#metodo`.
- Frontend: `npm install`, `npm run dev` on 3000, `npm run lint` (bare `eslint`, flat config). `/api/*` is proxied to `http://localhost:8080` by `next.config.ts`; override with `BACKEND_URL`.
- Typecheck: `npx tsc --noEmit`. On a fresh clone run `npm run build` or `npm run dev` first: `next-env.d.ts` and `.next/types/routes.d.ts` are gitignored but provide the global `PageProps` / `LayoutProps<...>` types.
- No frontend test runner exists; do not assume one.
- `npm start` (`next start`) does not work with `output: "standalone"`, which this app sets; use `npm run dev` locally or `node .next/standalone/server.js` after a build.
- Docker: `docker compose up -d --build` needs a root `.env` copied from `.env.example`; the Caddy service is behind `--profile ssl`. `docker compose down -v` deletes the database and uploaded files.

## Data-mutating checks

`docs/postman/SaludGrau.postman_collection.json` and `node docs/pruebas/prueba-carga.mjs http://localhost:8081` create rows and temporarily block an account. Run them only against a freshly installed test DB (`instalar_bd.ps1 -Reiniciar -BaseDeDatos centro_salud_prueba`, backend on 8081), never against `centro_salud`.

## Architecture

- Backend layers: `api/` (controllers, `dto/`, `seguridad/`), `service/`, `repository/` (Spring Data), `entity/` (JPA with explicit `@Column(name = "snake_case")`).
- Auth is not Spring Security: `TokenService` issues an HMAC-SHA256 `Bearer` token (12 h), `SesionArgumentResolver` turns it into the `UsuarioSesion` parameter, and role checks live in services as `sesion.exigirRol(...)`. PINs are BCrypt, matching the pgcrypto `crypt()` seeds.
- Errors: throw `ApiException` helpers. `ApiExceptionHandler` renders `{ "message": "<Spanish user-facing text>" }` for every status, and the frontend displays exactly that string, so keep the shape.
- DTOs are nested records in `api/dto/*Dto.java`; dates travel as `"YYYY-MM-DD"`, times as `"HH:mm"`.
- Spring Boot 4 uses Jackson 3: import `tools.jackson.databind.ObjectMapper`, not `com.fasterxml.jackson`. The web starter is `spring-boot-starter-webmvc`.
- `spring.jpa.hibernate.ddl-auto=update` is enabled, so entity edits can alter your local schema; the `sql/` scripts stay the source of truth for a clean install.
- Startup side effects: `HorarioGeneradorService` creates free slots 60 days ahead (`saludgrau.horarios.dias-a-futuro`), and `RecordatorioService` announces next-day appointments and campaigns at startup and hourly. Without `spring.mail.host` mail is only logged.
- Uploaded medical documents go to `citas_salud/archivos/` (gitignored).

## Frontend layers (keep the split)

- `src/lib/api/<modulo>.ts`: the `rutas` object used as SWR keys plus one function per request.
- `src/lib/api/types.ts`: hand-maintained mirror of the Java DTOs; update both sides in the same change.
- `src/hooks/use-<modulo>.ts`: SWR hooks over `rutas`.
- `src/components/<feature>/`: shadcn primitives live in `components/ui` and are added with `npx shadcn@latest add` (style `radix-nova`).
- `@/*` maps to `src/*`. The session lives in localStorage (`lib/auth/session-store.ts`) and is read through `useSesion()` (`undefined` means still hydrating); `PortalShell` redirects on role mismatch and `http.ts` clears the session on 401.

## Conventions

- Everything user-facing is Spanish: UI copy, validation and service messages, code comments, docs, and commit descriptions (pull-request titles usually end with `(#NN)`). Keep new strings Spanish, with no English fallback copy.
- Requirement IDs from the course spec (`RF-xx`, `RNF-xx`, `EDT x.y`) are cited in comments and test names; reuse them rather than inventing labels.
- Backend tests are Mockito unit tests with `DatosPrueba` helpers and no Spring context, except the `integracion`-tagged one.
- JasperReports: edit only `citas_salud/src/main/resources/reports/*.jrxml` (PascalCase, JasperReports 7 format). `PlantillasClinicasTest` compiles each SQL template and must stay green.

## Gotchas

- Never commit `citas_salud/config/` or `.env*`: `config/application.properties` holds each machine's PostgreSQL password (the installer reads it first) and `.env` holds production secrets.
- The four lowercase `.jrxml` files at the repo root and the root `04_datos_prueba.sql` are unused leftovers; the live templates and seed data are under `citas_salud/src/main/resources/reports/` and `sql/`.
- `.gitattributes` forces LF on `*.sh`; CRLF breaks the Docker first-load entrypoint.
- Containers run with `TZ=America/Lima`, and "today" logic, the reminder cron, and seed dates assume that timezone.
- Admin accounts cannot be registered from the UI; they exist only in the seeded SQL data.
- `citas_salud/target/` and `frontend/.next/` are gitignored build outputs; don't treat them as source.
- There is no CI, no Java formatter or linter, and no Gradle in this repo; don't invent `gradlew` or `spotless` steps.
