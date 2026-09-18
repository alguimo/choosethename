# AGENTS.md — Collaborative Name Decider

## Project
A collaborative web application designed to help groups of people choose a name through a structured workflow of suggestions, grouping, and voting rounds. The project is decoupled, featuring an Angular frontend (TypeScript, RxJS) and a Java Spring Boot backend using MapStruct for database and entity mapping.

## Repository structure
- `specs/` — SDD documentation: constitution, system spec, contracts, task plans, `openapi.yaml`, and subsystem specs (001-006).
- `src/backend/` — Spring Boot production source (Java + `resources/`, incl. Flyway migrations).
- `src/frontend/` — Angular application.
- `tests/backend/` — Backend unit and integration tests (JUnit) + test resources.
- `tests/frontend/` — Frontend spec files (Karma/Jasmine), mirroring the `app/` tree.

## Commands
All commands run from the repository root.
- Run:
  - Backend: `mvn spring-boot:run`
  - Frontend: `npm start`
- Tests:
  - Backend: `mvn test`
  - Frontend: `npm test`
- Lint/Format:
  - Backend: `mvn spotless:apply` (Spotless plugin is not yet configured in `pom.xml`)
  - Frontend: `npm run lint`

## Style and conventions
- **Language versions**: Java 17+, TypeScript 5.x, Angular 17+ (with Standalone Components).
- **Naming conventions**: 
  - Backend: PascalCase for classes, camelCase for variables/methods, snake_case for database tables and columns.
  - Frontend: camelCase for variables/methods, kebab-case for component selectors and filenames.
- **Language policy**: All code, variable/function naming, commit messages, code comments, documentation, and markdown (`.md`) files must be written strictly in **English**. Only the user interface (UI) text shown to final users will be in **Spanish**.

## Rules
- Read `specs/00-constitution.md` and the active specification file in `specs/` (the highest-numbered approved spec) before touching any code or configuration.
- **Limits**: Do not add external dependencies, libraries, or frameworks in `pom.xml` or `package.json` without asking the user first. In case of any ambiguity in the specification requirements, STOP work immediately and ask for clarification.

## Upon finishing any task
- Ensure an automated test has been defined and executed for the specific task before finishing the implementation (Test-First principle).
- Run the full project test suites (both Backend and Frontend) and verify they pass 100%.
- Run linters and formatters to ensure zero warnings and proper code formatting.
