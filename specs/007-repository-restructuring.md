# Spec 007 — Repository Restructuring & Clean SDD Packaging

## Context & Objectives
The repository grew organically: the Angular frontend lives at the project root (`src/`), the Spring Boot backend under `backend/` with Maven's default `src/main` / `src/test` layout, specs are split between `specs/` and `docs/`, and a Maven build cache (`backend/target/`) was accidentally committed to Git. This spec restructures the repository into a professional Spec-Driven Development (SDD) hierarchy that centralizes specifications and plans under `/specs`, isolates production source under `/src`, and separates tests under `/tests`, preserving full functional integrity (100% green suites before and after) and producing a clean, atomic Git history.

## Users / Actors
* **Contributor**: A human or AI agent that reads, extends, or maintains the repository.

## User Stories
* **US-1**: As a Contributor, I want all specifications, contracts, and plans centralized under `specs/`, so the SDD documentation lives in one discoverable place.
* **US-2**: As a Contributor, I want all production source code under `src/`, so I can locate implementation by layer regardless of technology.
* **US-3**: As a Contributor, I want all unit and integration test suites under `tests/`, so tests are cleanly separated from production code.
* **US-4**: As a Reviewer, I want a clean atomic Git history, so each restructuring concern maps to a focused, independently verifiable commit.

## Functional Requirements (Acceptance Criteria in EARS format)
* **FR-1 (Ubiquitous)**: THE SYSTEM MUST keep 100% of the existing test suites (backend and frontend) green before any file is moved and after every restructuring commit.
* **FR-2 (Ubiquitous)**: THE SYSTEM MUST NOT modify any business logic, domain code, API contract, or data during the restructuring; only file layout and build/path configuration may change.
* **FR-3 (Event-driven)**: WHEN the restructuring completes, THEN the backend suite MUST run green from the new layout (`mvn test`), the frontend suite MUST run green (`npm test`), and lint MUST report zero warnings (`npm run lint`).
* **FR-4 (Event-driven)**: WHEN a tracked file is a build cache, a terminal log, or a secret (`.env`), THEN THE SYSTEM MUST remove it from the Git index and keep it ignored.
* **FR-5 (Ubiquitous)**: THE SYSTEM MUST publish the restructuring as four atomic commits: (1) specs/docs consolidation, (2) backend restructure, (3) frontend restructure, (4) final hygiene (README/AGENTS/`.gitignore`).

## Non-Functional Requirements
* **NFR-1 (Paths)**: Production source lives in `src/backend/` (Java + resources) and `src/frontend/` (Angular app). Tests live in `tests/backend/` and `tests/frontend/`. Specifications and plans live in `specs/`.
* **NFR-2 (Language)**: All code, configuration, documentation, and commit messages MUST be written in English.
* **NFR-3 (Dependencies)**: The restructuring MUST NOT add external dependencies, libraries, or frameworks to `pom.xml` or `package.json`.

## Edge Cases
* **Karma test discovery**: If the Angular karma builder does not auto-discover specs relocated under `tests/frontend/`, the `tsconfig.spec.json` include globs (or a custom `karmaConfig`) MUST be adjusted so exactly all relocated specs execute.
* **Maven default layout assumptions**: Because both source and test directories leave Maven's conventional layout, `pom.xml` MUST declare explicit `sourceDirectory`, `testSourceDirectory`, `<resources>`, and `<testResources>` paths so Flyway still resolves `classpath:db/migration` and tests find their `tests/backend/resources/application.yml`.
* **Tracked build artifacts**: `backend/target/` (195 files) is currently in the Git index; it MUST be removed with `git rm -r --cached` and ignored going forward.

## Out of Scope
* Adding a Maven Wrapper (`mvnw`) or the Spotless plugin (would introduce new files/tooling; requires explicit approval).
* Any functional, behavioral, or specification-content change to specs 001–006, backend domain logic, or frontend features.
* Converting the Angular workspace into a separate library package or monorepo tool.

## Completion Criteria
* All FR-1..FR-5 honored; backend and frontend suites pass 100% with zero lint warnings from the new layout.
* The target tree exists: `specs/` (00-constitution, 01-system-spec, 02-contracts, 03-task-plan, 001–007 + `openapi.yaml`), `src/backend`, `src/frontend`, `tests/backend`, `tests/frontend`.
* Git index contains no build cache, log, or secret; history shows four atomic commits.