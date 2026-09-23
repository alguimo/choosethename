# Choosethename

Collaborative web application that helps groups choose a name through a structured workflow of **suggestions, selection, voting rounds, and results**. A group forms a *list* with an invitation code, members propose *names*, common and faded suggestions are matched, participants vote through elimination rounds, and the system exposes the final ranked result.

## Architecture

Two strictly decoupled layers communicating over stateless JSON REST contracts (`specs/openapi.yaml`):

- **Backend** (`src/backend`, `tests/backend`): Spring Boot 3.x (Java 17), MapStruct, Flyway migrations, BCrypt/JWT security.
- **Frontend** (`src/frontend`, `tests/frontend`): Angular 17 (standalone components), Angular Material + CDK, RxJS.

Specifications and plans follow Spec-Driven Development and live under `specs/` (see `specs/00-constitution.md`, `specs/01-system-spec.md`, `specs/02-contracts.md`, `specs/03-task-plan.md`).

## Repository layout

```
specs/            SDD documentation (constitution, system, contracts, plans, openapi.yaml, subsystem specs 001-006)
src/backend/      Spring Boot production sources (Java + resources + Flyway migrations)
src/frontend/     Angular application (app/, assets/, index.html, main.ts, styles.scss)
tests/backend/    Backend unit & integration tests (JUnit) + test resources
tests/frontend/   Frontend spec files (Karma/Jasmine), mirroring the app tree
```

## Development server

The backend reads its configuration from environment variables (see `.env.example`; copy it to `.env` and fill the values):

- `BDD_URL`, `BDD_USERNAME`, `BDD_PASSWORD`, `BDD_DRIVER` — PostgreSQL connection (local DB via `docker-compose.yml`).
- `JWT_SECRET` — **required**, base64-encoded secret of at least 32 bytes (the app fails fast at startup without it).
- `JWT_EXPIRATION_MS` — token lifetime in ms (default `86400000`).
- `CORS_ALLOWED_ORIGINS` — comma-separated origins allowed by the backend (default: none, so set `http://localhost:4200` for local development).

Load the values into the shell (e.g. `set -a; source .env; set +a`) and then:

- Backend: `mvn spring-boot:run` (serves the API on `http://localhost:8080`).
- Frontend: `npm start` (serves the Angular app on `http://localhost:4200`, proxying `/api` to the backend on `8080` via `proxy.conf.json`).

## Build

- Backend: `mvn package`
- Frontend: `npm run build` (output to `dist/choosethename`)

## Tests

- Backend: `mvn test`
- Frontend: `npm test`

Run linters/formatters: `mvn spotless:apply` (plugin not yet configured in `pom.xml`) and `npm run lint`.

## Database

A local PostgreSQL (16) is provided via `docker-compose.yml` (or the duplicate at `bdd/docker-compose.yml`). The backend applies schema migrations with Flyway on startup. Connection values come from the `BDD_*` environment variables documented above.