# System Spec — Collaborative Name Decider

## Overview
The system is a collaborative web application that helps groups of people choose a name through a structured workflow: **suggestion → selection → voting → results**. A group forms a *list* with an invitation code, members contribute candidate *names*, common and faded suggestions are matched, participants vote through elimination rounds, and the system exposes the final ranked result.

## Actors
* **Administrator**: Creates Participant accounts with a username and password.
* **Participant**: Authenticates, joins or creates lists, suggests names, selects, ranks, and views results.
* **End User**: Any Participant interacting with the Spanish-language UI.

## Architecture
The system is built on two strictly decoupled layers communicating over stateless JSON REST contracts (see `02-contracts.md`, source of truth `openapi.yaml`):

* **Frontend**: Angular 17 (standalone components) at `src/frontend/`. Presentation only; JWT is held client-side; local state is buffered in `localStorage` with a single sync per phase.
* **Backend**: Spring Boot 3.x (Java 17) at `src/backend/`. All business logic, phase transitions, rankings, and persistence live here; schema is versioned with Flyway migrations.

## Governance
* The **Constitution** (`00-constitution.md`) is the binding set of principles: strict SDD, strict frontend/backend decoupling, mandatory test-first verification, English engineering language with Spanish UI, technology guidance via `AGENTS.md`.
* Subsystem specifications 001–006 define the detailed functional contracts:
  * `001-backend-foundation.md` — registration, authentication, JWT, migrations.
  * `002-backend-lists.md` — list creation, joining, invitation codes, membership.
  * `003-backend-names.md` — name suggestions, normalization, phase transitions, selection.
  * `004-backend-voting.md` — voting rounds, Borda ranking, results, timeouts.
  * `005-frontend-ui.md` — application flows.
  * `006-ui-kit.md` — reusable component library (Atomic Design).
  * `007-repository-restructuring.md` — the active spec for the repository layout refactoring.

## Core Domain States
A list progresses through the phases `ADDITION → SELECTION → VOTING → COMPLETED`, with a terminal `EXPIRED` state triggered by 48-hour inactivity timeouts. Voting is structured in rounds with a full ranking of the current pool per member; rounds advance only when all members have voted.

## Repository Layout
```
specs/   SDD documentation (constitution, system, contracts, planning, openapi.yaml, subsystem specs)
src/     Production source: src/backend (Java + resources), src/frontend (Angular app)
tests/   Test suites: tests/backend (JUnit), tests/frontend (Karma/Jasmine specs)
```
Implementation and operation commands are defined in `AGENTS.md`.