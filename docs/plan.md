# Development Plan: Backend Lists and Sharing (Spec 002)

## 1. Structure & Architecture
- **Root**: Spring Boot backend located at `backend/src/main/java/com/choosethename/backend`.
- **Packages**:
  - `api`: `ListController`
  - `dto`: `CreateListRequestDTO`, `JoinListRequestDTO`, `ListResponseDTO`, `ListMapper`
  - `model`: `ListEntity`, `ListMembershipEntity`, `ListPhase`
  - `repository`: `ListRepository`, `ListMembershipRepository`
  - `service`: `ListService`, `InvitationCodeGenerator`
  - `exception`: `GlobalExceptionHandler` mapping domain exceptions to HTTP statuses
- **Database Migrations**: `backend/src/main/resources/db/migration/V2__create_lists_tables.sql`
- **Tests**: `backend/src/test/java/com/choosethename/backend`

---

## 2. Implementation Tasks (<30 min each)

- [x] **Task 1: Flyway Migration for Lists and Memberships**
  - **Description**: Create Flyway SQL migration `V2__create_lists_tables.sql` for `lists` and `list_memberships` tables with appropriate constraints, foreign keys, and indexes.
  - **RF**: NFR-1, FR-1, FR-4, FR-10.
  - **Done when**: Flyway applies migration successfully and `ListsTableMigrationTest` passes.

- [x] **Task 2: Domain Entities, DTOs & MapStruct Mappers**
  - **Description**: Create `ListEntity`, `ListMembershipEntity`, `ListPhase` enum, DTOs (`CreateListRequestDTO`, `JoinListRequestDTO`, `ListResponseDTO`), and MapStruct `ListMapper`.
  - **RF**: NFR-2, FR-1, FR-15.
  - **Done when**: `ListMapperTest` passes, mapping entities to `ListResponseDTO` including member usernames and expiration timestamp.

- [x] **Task 3: Invitation Code Generator & Repositories**
  - **Description**: Implement `InvitationCodeGenerator` (generating unique 6-character uppercase alphanumeric strings) and Spring Data JPA repositories `ListRepository` and `ListMembershipRepository` with active list lookup methods.
  - **RF**: FR-1, FR-4, FR-6, FR-7, FR-16.
  - **Done when**: Unit tests for `InvitationCodeGenerator` and custom repository methods pass 100%.

- [x] **Task 4: List Service — Creation & Active Retrieval**
  - **Description**: Implement `ListService` logic for list creation (`createList`) with 48h expiration and active list retrieval (`getActiveList`). Enforce restriction preventing users from having multiple active lists.
  - **RF**: FR-1, FR-2, FR-3, FR-15, FR-16.
  - **Done when**: `ListServiceTest` passes for successful creation, blank name validation, duplicate active list rejection, and active list fetch.

- [x] **Task 5: List Service — Joining & Closing Invitations**
  - **Description**: Implement `ListService` logic for `joinList` and `closeInvitations`. Enforce 5-member cap, expiration check, case-insensitive code matching, auto-close on 5th member, and owner-only permission for closing invitations.
  - **RF**: FR-4, FR-5, FR-6, FR-7, FR-8, FR-9, FR-10, FR-11, FR-12, FR-13, FR-14.
  - **Done when**: `ListServiceTest` passes for join scenarios (success, expired code, closed invitations, 5-member cap, duplicate member) and invitation closure scenarios.

- [x] **Task 6: List API Controller & Integration Tests (TDD Verification)**
  - **Description**: Create `ListController` mapping REST endpoints (`POST /api/v1/lists`, `POST /api/v1/lists/join`, `GET /api/v1/lists/active`, `PATCH /api/v1/lists/{id}/close-invitations`). Write integration test suite covering `TS-1` through `TS-14`.
  - **RF**: FR-1 through FR-16, TS-1 to TS-14.
  - **Done when**: Full integration test suite `ListControllerIntegrationTest` passes 100% covering all `TS-1` to `TS-14` test scenarios.

---

# Development Plan: Backend Names Addition and Selection (Spec 003)

## 1. Structure & Architecture
- **Root**: Same Spring Boot backend at `backend/src/main/java/com/choosethename/backend`.
- **Packages**:
  - `model`: `NameEntity`, `SharedNamePoolEntity`
  - `dto`: `AddNameRequestDTO`, `NameResponseDTO`, `SelectionResponseDTO`
  - `repository`: `NameRepository`, `SharedNamePoolRepository`
  - `service`: `NameService`, `NameNormalizer` (normalization utility), `ListPhaseTransitionService` (transitions + 48h timeout)
  - `exception`: `EmptyAdditionException`, `DuplicateNameException`, `ConcurrentTransitionException` handled by `GlobalExceptionHandler`
  - `config`: scheduled job for the 48-hour timeout check
- **Database Migrations**: `backend/src/main/resources/db/migration/V3__create_names_tables.sql`
- **Tests**: `backend/src/test/java/com/choosethename/backend`

---

## 2. Implementation Tasks (<30 min each)

- [x] **Task 1: Flyway Migration, Entities & Domain States (~25 min)**
  - **Description**: Create SQL migration `V3__create_names_tables.sql` for `names` and `shared_name_pool` tables (with FKs to users/lists), add a `version` column to `lists` for optimistic locking, and add `NameEntity`, `SharedNamePoolEntity` plus the `SELECTION`, `VOTING` and `EXPIRED` phase values.
  - **RF**: FR-2, FR-5, FR-6, FR-8, NFR-4.
  - **Done when**: Flyway applies the migration cleanly and an integration test verifies the `names`, `shared_name_pool` and `lists.version` schema; entities compile with `@Version` on `ListEntity`.

- [x] **Task 2: Normalization Service (NFR-1) (~20 min)**
  - **Description**: Implement `NameNormalizer` as the single source of truth: trim, collapse consecutive whitespace into a single space, strip accents (Java `Normalizer`, NFD, remove diacritics), convert to lowercase; detect names that normalize to an empty string.
  - **RF**: NFR-1, FR-2, FR-3.
  - **Done when**: `NameNormalizerTest` passes 100% for trim, whitespace collapse, accent stripping, lowercase, and empty-after-normalization cases (e.g. `"  PABLO  García  "` → `"pablo garcia"`, `"   "` → rejected as empty).

- [x] **Task 3: Name Addition Service & API (~25 min)**
  - **Description**: Implement `NameService.add` (normalize and persist per user/list), `nameRepository` queries, `NameController` endpoints (`POST /api/v1/lists/{id}/names`), duplicate rejection (422), finish-addition endpoint enforcing `FR-9` (400 with exact English message), and privacy filtering per `NFR-2` and `NFR-3`.
  - **RF**: FR-1, FR-2, FR-3, FR-9, NFR-2, NFR-3.
  - **Done when**: Integration tests pass for: normalized persisted name, duplicate returns 422, finish with zero names returns 400 with the exact message "At least one name must be provided to proceed to the selection phase.", and the other participant's names are never exposed during ADDITION.

- [x] **Task 4: Phase Transitions & 48-Hour Timeout (~25 min)**
  - **Description**: Implement `ListPhaseTransitionService` performing final, irreversible transitions (ADDITION→SELECTION when both FINISHED, SELECTION→VOTING when both COMPLETED) with optimistic locking, plus a scheduled job that transitions ADDITION lists older than 48 hours to EXPIRED.
  - **RF**: FR-4, FR-7, FR-8, NFR-4.
  - **Done when**: Integration tests verify both transitions fire once both participants confirm, a list aged beyond 48 hours in ADDITION becomes EXPIRED, and each transition increments the `version` column.

- [x] **Task 5: Selection & Matching, Concurrency & Full Verification (~25 min)**
  - **Description**: Implement `SelectionResponseDTO` computation (Common Names vs. Faded Suggestions), adopt-from-faded endpoint (FR-6) adding to the shared pool, concurrency handling (second concurrent transition gets 409 Conflict), and run the entire backend + frontend suites, linters and formatters.
  - **RF**: FR-5, FR-6, FR-4, FR-7, NFR-1, NFR-3, NFR-4.
  - **Done when**: Integration tests return correct Common/Faded lists and adopt a faded name into `shared_name_pool`; a concurrent-transition test yields exactly one success and one 409; full backend test suite passes 100% with zero lint/format warnings.

---

# Development Plan: Backend Voting Rounds (Spec 004)

## 1. Structure & Architecture
- **Root**: Same Spring Boot backend at `backend/src/main/java/com/choosethename/backend`.
- **Packages**:
  - `model`: `VotingRoundEntity`, `VoteEntity`
  - `dto`: `VoteRequestDTO`, `ResultsResponseDTO`, `VoteMapper`, `VotingRoundMapper`
  - `repository`: `VotingRoundRepository`, `VoteRepository`
  - `service`: `VotingService` (submit, consolidation, round transitions), `RankingService` (Borda scoring + elimination + tie-breaks), extended `ListPhaseTransitionService` (48h VOTING→EXPIRED)
  - `controller`: `VotingController` (`POST /api/v1/lists/{id}/vote`, `GET /api/v1/lists/{id}/results`)
  - `config`: scheduled job extended for the VOTING timeout
- **Database Migrations**: `backend/src/main/resources/db/migration/V6__create_voting_tables.sql`
- **Approved decisions (defaults closed)**:
  - **Scoring**: Borda count per round: points = `pool_size − rank_position`; consolidated score = sum over all voters of the list (up to 5).
  - **Pool**: voted names = `shared_name_pool` entries; the >15/<=15 branch is evaluated once at VOTING entry.
  - **Round model**: `phase="VOTING"` + new `lists.current_round` (initial `1`); FR-2 increments the round, never changes the phase string.
  - **Participants**: up to 5 voters; a round advances only when ALL members have voted.
  - **Elimination**: hard cap at the threshold (top 10 / top 5 / top 3); ties at the cap broken alphabetically by normalized name (NFR-2); ties for ranking positions 1-2-3 also resolved per NFR-2.
  - **Tie/less-than-3**: if the surviving pool holds fewer names than the cap, the round/result contains the remaining names (resolves FR-3 vs. the "Round Finalization" edge case).
  - **Vote semantics**: full ranking of the current round's pool is mandatory (missing names → 400); duplicate name in a ranking → 422; re-vote in the same round overwrites; a vote for an already-advanced round → 409.
  - **Concurrency**: round consolidation/transition guarded by optimistic locking; concurrent triggers → exactly one success, the other 409 (extends 003 NFR-4).
  - **Results**: new `GET /api/v1/lists/{id}/results` (member-only, 403 otherwise; 409 if list not COMPLETED), exposes ordered top-3 (or fewer) with scores.
  - **Timeout**: lists in VOTING beyond 48 hours transition to terminal EXPIRED (mirrors the ADDITION timeout).
  - **Spec 003 ripple**: matching generalized to up to 5 private pools; "Common Names" = present in more than half the pools (simple majority); "Faded Suggestions" = present in at least one pool.
  - **Authorization**: all 004 endpoints require a valid JWT and list membership (002 NFR-1).

## 2. Implementation Tasks (<30 min each)

- [x] **Task 1: Respec Spec 004 + correct 002/003 conflicts (~25 min)**
  - **Description**: Rewrite `specs/004-backend-voting.md` encoding every approved decision above: Borda formula, `current_round` model, up-to-5-voter gate, API contract (`POST /vote`, `GET /results`), edge cases, authorization, TDD test scenarios section. Patch 003 Domain States (VOTING is non-terminal → COMPLETED) and 002 phase-vocabulary notes.
  - **RF**: Constitution P1.1, P3.1; FR-1..FR-5, NFR-1, NFR-2.
  - **Done when**: No ambiguity from the QA review remains open in the specs; the spec documents scoring, round model, endpoints, edge cases and initial test scenarios.

- [x] **Task 2: Flyway Migration V6 — Voting Tables & current_round (~20 min)**
  - **Description**: Create `V6__create_voting_tables.sql`: `voting_rounds` (id, list_id FK, round_number, created_at, UNIQUE(list_id, round_number)) and `votes` (id, list_id FK, user_id FK, round_id FK, rankings, submitted_at, UNIQUE(list_id, user_id, round_id)); `ALTER TABLE lists ADD COLUMN current_round INT NOT NULL DEFAULT 1`.
  - **RF**: NFR-1, FR-1.
  - **Done when**: Flyway applies the migration cleanly and a migration test verifies the schema (tables, FKs, UNIQUE indexes, `lists.current_round`).

- [x] **Task 3: Entities, DTOs, Repositories & MapStruct Mappers (~25 min)**
  - **Description**: Add `VotingRoundEntity`, `VoteEntity`, `VoteRequestDTO`, `ResultsResponseDTO`, `VotingRoundMapper`, `VoteMapper`; `VotingRoundRepository`, `VoteRepository` (find by list+round, by list+user+round, delete/replace for overwrite).
  - **RF**: NFR-1, FR-1, NFR-2.
  - **Done when**: Entities compile against `V6` schema (`ddl-auto: validate`), `VoteMapperTest` passes for request→entity mapping, and repositories return correct rows for the round/participant queries.

- [x] **Task 4: RankingService — Borda Scoring, Elimination & Tie-breaks (~25 min)**
  - **Description**: Implement pure scoring: per-voter Borda points, consolidated sum over voters, deterministic ranking with alphabetical (normalized name) tie-break, hard-cap elimination (10/5/3 branch by initial pool size), and degraded caps when the pool is smaller than the threshold.
  - **RF**: FR-1, FR-4, FR-5, NFR-2.
  - **Done when**: `RankingServiceTest` passes 100% covering >15 (16→10→5→3), <=15 (15→5→3), exhaustive/partial rankings, alphabetical tie breaks at caps and for 1-2-3, and <3-name pools.

- [x] **Task 5: VotingService — Submit, Consolidation & Round Transitions (~30 min)**
  - **Description**: Implement `submitVote` (validate membership/phase, full ranking (400), duplicate names (422), re-vote overwrite, stale-round 409) and `consolidateWhenAllVote` (advance `current_round`, transition to COMPLETED on last round, expose results), guarded by optimistic locking so concurrent triggers yield one success + one 409.
  - **RF**: FR-1, FR-2, FR-3, NFR-1; 003 NFR-4 extension.
  - **Done when**: Service integration tests pass: partial voting does not advance the round, all-vote advances/ completes, stale votes rejected with 409, overwrite persists, and a concurrent final trigger yields exactly one transition and one 409.

- [x] **Task 6: Results Exposure — GET /lists/{id}/results (~20 min)**
  - **Description**: Implement `ResultsResponseDTO` assembly and the results query from the final round: ordered names (top-3 or fewer, per NFR-2) with consolidated scores; 403 for non-members, 409 while not COMPLETED.
  - **RF**: FR-3, NFR-2; 002 NFR-1.
  - **Done when**: Service/controller tests pass for ordered top-3, fewer-than-3 degradation, non-member 403 and not-completed 409.

- [x] **Task 7: VOTING 48-Hour Timeout (~15 min)**
  - **Description**: Extend the scheduled job and `ListPhaseTransitionService` so lists in VOTING older than 48 hours transition to terminal EXPIRED (mirroring the ADDITION rule), including the `updatedAt`/version guard.
  - **RF**: FR-2, 003 FR-8 pattern.
  - **Done when**: An integration test ages a VOTING list beyond 48h and asserts the phase becomes EXPIRED.

- [x] **Task 8: Selection Majority Matching for up to 5 Pools (Spec 003 ripple) (~25 min)**
  - **Description**: Generalize `SelectionService` Common/Faded computation from 2 pools to up to 5: Common if a normalized name appears in more than half the member pools, Faded if in at least one; keep adoption and dedup unchanged; adapt existing 2-participant tests.
  - **RF**: 003 FR-5, FR-6.
  - **Done when**: `SelectionServiceTest` passes for 2-, 3-, 4- and 5-member pools with majority-common and faded lists correct, and adopt-from-faded still works.

- [x] **Task 9: Voting Controllers & Integration Tests (TDD Verification) (~30 min)**
  - **Description**: Add `VotingController` (`POST /api/v1/lists/{id}/vote`, `GET /api/v1/lists/{id}/results`) and a RestAssured suite covering every initial test scenario of the rewritten Spec 004: submit/overwrite/stale, incomplete (400) and duplicate (422) rankings, 2- and 5-member rounds, both >15 and <=15 branches, tie-breaks, concurrency 409, results 403/409, timeout→EXPIRED.
  - **RF**: FR-1..FR-5, NFR-1, NFR-2; Constitution P3.1.
  - **Done when**: All 004 test scenarios pass 100% against a real HTTP endpoint stack (`RANDOM_PORT`, H2, Flyway).

- [x] **Task 10: Full Verification (~20 min)**
  - **Description**: Run the entire backend suite (`./mvnw test`), frontend suite (`npm test`, `npm run lint`) and `./mvnw spotless:apply`; confirm zero warnings and 100% green.
  - **RF**: Constitution P3.2; AGENTS.md finishing rules.
  - **Done when**: Backend and frontend test suites pass 100%; linters and formatters report zero warnings; no spec contradiction remains between 002/003/004.

---

# Development Plan: Frontend Application (Spec 005) + UI Kit Library (Spec 006)

These two specs are developed **in parallel**: the `ui-kit` component inventory (Spec 006) is driven by the concrete needs of the application flows (Spec 005). Each UI Kit component is built to satisfy a specific FR from Spec 005, so it is test-first and consumed immediately by the features that require it.

## 1. Structure & Architecture
- **Workspace**: The frontend lives at the project root (single Angular 17 workspace). The UI Kit is a folder `src/app/ui-kit` inside the same project (extractable in the future; not a separate library package for now — approved decision).
- **UI Kit (`src/app/ui-kit`) — Atomic Design (Spec 006)**:
  - **Atoms**: `button`, `input-field`, `icon-button`, `badge`, `validation-message`.
  - **Molecules**: `name-input-row`, `list-card`, `phase-indicator`, `round-indicator`.
  - **Organisms**: `draggable-ranking-list` (Angular CDK), `modal`.
  - **tokens/**: `_variables.scss` with CSS custom properties (design tokens, `--ui-*`).
- **Features (`src/app/features`) — business flows (Spec 005)**:
  - `auth/`: login screen, JWT storage, HTTP interceptor (auto-attach `Authorization: Bearer`).
  - `dashboard/`: active-list card / empty state, create-list modal, join-list modal, 5-minute refresh.
  - `suggestion/`: local-first name input, validation (chars + duplicates), finish-addition sync.
  - `selection/`: selection view (common / faded / own names), adopt flow, complete-selection.
  - `voting/`: round-based drag-and-drop ranking, submit vote, stale/invalid handling.
  - `results/`: final results screen.
- **Services (`src/app/services`)**: `api.service` (HTTP wrappers for every `openapi.yaml` operation), `local-storage.service` (namespaced per-list buffer, `list_{id}_suggestions`, `list_{id}_vote_round_{n}`), `auth.service` (session, re-auth at phase completion).

## 2. Data Model (API Contract)
- Models mirror `openapi.yaml` DTOs directly; no frontend-side normalization (backend is single source of truth per 003 NFR-1).
- `ListResponse` (id, name, invitationCode, codeExpiresAt, phase, invitationsOpen, ownerUsername, currentRound, totalRounds, currentPool, members) — root state for dashboard and phase routing.
- `NameEntry` (name, normalizedName) — additions and selection view.
- `AddNameRequest` (names[]) — bulk sync at finish-addition.
- `VoteRequest` (roundNumber, rankings[]) — per-round vote submission.
- `ResultsResponse` (results: rank, name, score) — results screen.
- `Error` (error) — backend messages surfaced inline; UI text translated to Spanish.

## 3. Justified Decisions (discarded alternatives included)

| Decision | Rationale | Discarded Alternative |
| :--- | :--- | :--- |
| **Angular Material + CDK** | Official Angular ecosystem, strong a11y (WCAG 2.1 AA) and behavioral primitives (drag-drop, overlay, focus trap) with zero custom JS. | **ng-zorro**: heavy, non-native to Angular, extra theming/config cost for the same behaviors. |
| **UI Kit inside the same project (`src/app/ui-kit`)** | No build complexity now; the kit stays decoupled (no app imports) so it can be extracted into an npm package later. | **Separate Angular Workspace library (`ng-packagr`)**: adds release/versioning overhead that is not needed today. |
| **Atomic Design (atoms→molecules→organisms)** | Max reuse: page flows compose molecules/organisms, atoms stay stateless and themeable. | **Monolithic page components**: duplicated markup, hard to test, no reuse. |
| **localStorage as local buffer, single sync at phase end** | Offline resilience + delayed auth (005 FR-4); full state sent once on "Terminar Fase" / "Enviar voto". | **WebSockets realtime sync**: out of scope, infra complexity, unnecessary for MVP. |
| **Dedicated `local-storage.service` namespaced per list/round** | Isolates cache concerns, trivial clear-on-success / retain-on-error. | **NgRx/global store**: overkill for the buffered local state we actually manage. |
| **Angular Reactive Forms for all forms** | Declarative validation control, easy inline error wiring. | **Template-driven forms**: harder to handle cross-field validation and dynamic error messages. |

## 4. Test Strategy (TDD)
- **Per component/service, tests are defined first** (`.spec.ts`) then implementation is written to satisfy them (Constitution P3.1).
- **UI Kit tests**: `TestBed` — render with minimal inputs, assert primary interaction emits the expected `@Output` (NFR-5, min 2 tests per component, see 006 TS-1..TS-17).
- **Feature/services tests**: isolated Angular tests with mocked `HttpClient` (`HttpTestingController`) covering success/failure for every API call and every cache policy (clear-on-success, retain-on-error).
- **Auth tests**: interceptor attaches token; 401 during phase completion triggers re-auth modal then retries.
- **LocalStorage tests**: namespacing, restore on entry, stale-ranking discard (FR-33), unavailable-storage warning (FR-46).
- **Final gate**: `npm test` 100% green, `npm run lint` zero warnings (Constitution P3.2, AGENTS.md). E2E is out of scope for the MVP (candidate for a future spec).

## 5. RF Coverage per Part

| Module / Part | Spec / RFs Covered |
| :--- | :--- |
| **ui-kit atoms + molecules + organisms** | 006 FR-1..FR-38; 005 FR-48, FR-50 (consumed, not re-implemented) |
| **auth feature + interceptor** | 005 FR-1, FR-2, FR-3, FR-4, FR-5; NFR-4 |
| **dashboard feature** | 005 FR-6, FR-7, FR-8, FR-9, FR-10, FR-11, FR-12, FR-13, FR-14, FR-15, FR-16, FR-17 |
| **suggestion feature** | 005 FR-18, FR-19, FR-20, FR-21, FR-22, FR-23, FR-24, FR-25, FR-26 |
| **selection feature** | 005 FR-27, FR-28, FR-29, FR-30, FR-31 |
| **voting feature** | 005 FR-32, FR-33, FR-34, FR-35, FR-36, FR-37, FR-38, FR-39 |
| **results feature** | 005 FR-40, FR-41, FR-42 |
| **local-storage.service** | 005 FR-43, FR-44, FR-45, FR-46, FR-47 |
| **cross-cutting (NFRs)** | 005 NFR-1 (decoupling), NFR-2 (language policy), NFR-3 (state), NFR-6 (perf), NFR-7 (API alignment) |

## 6. Implementation Tasks (Spec 005 + 006, parallel)

- [x] **Task 1: Scaffold Angular 17 project (~30 min)**
  - **Description**: Create Angular 17 standalone-components app (no NgModules), add routing, install `@angular/material` and `@angular/cdk`, configure `angular.json` + base styles. Verify it builds (`ng build`) and runs the default smoke test.
  - **RF**: 005 FR-48, FR-49, FR-51; NFR-6.
  - **Done when**: `npm start` serves a blank routed app; `npm test` green; `npm run lint` zero warnings.

- [x] **Task 2: UI Kit design tokens (~15 min)**
  - **Description**: Create `tokens/_variables.scss` with all `--ui-*` custom properties (color, radius, spacing, typography) and default fallbacks; wire them into global styles.
  - **RF**: 006 FR-3; Edge case "theme tokens not defined".
  - **Done when**: Tokens compile and components render with defaults when the app defines no overrides.

- [x] **Task 3: Atoms — button, icon-button, input-field (~30 min)**
  - **Description**: Implement `ui-button` (variants, loading spinner, disabled, `clicked`), `ui-icon-button` (icon, tooltip, danger variant, `clicked`), `ui-input-field` (label, placeholder, value, error→red border+message, `valueChanged`, `submitted` on Enter), all OnPush standalone.
  - **RF**: 006 FR-1..FR-16; 005 FR-19 (red-border error surfaced via this input).
  - **Done when**: 006 TS-1, TS-2, TS-3, TS-4, TS-5, TS-17 pass.

- [x] **Task 4: Atoms — badge, validation-message (~15 min)**
  - **Description**: Implement `ui-badge` (default/success/warning/danger color variants) and `ui-validation-message` (error/warning/info with icon + color).
  - **RF**: 006 FR-17..FR-20.
  - **Done when**: 006 TS-6, TS-7 pass.

- [x] **Task 5: Molecules — name-input-row, list-card, phase-indicator, round-indicator (~30 min)**
  - **Description**: Implement `ui-name-input-row` (input + send arrow; emits `nameSubmitted` with trimmed value; blocks empty/whitespace; 300ms debounce), `ui-list-card` (title, phase badge, member count, clickable), `ui-phase-indicator` (stepper), `ui-round-indicator` ("Ronda X de Y").
  - **RF**: 006 FR-21..FR-29; 005 FR-7, FR-32.
  - **Done when**: 006 TS-8, TS-9, TS-10, TS-11, TS-12 pass.

- [x] **Task 6: Organisms — draggable-ranking-list, modal (~30 min)**
  - **Description**: Implement `ui-draggable-ranking-list` (CDK drag-drop, `items`/`disabled` inputs, `rankingsChanged` output, lift shadow + drop-zone feedback, empty-items edge case) and `ui-modal` (title, visible, `closed` on Escape/backdrop/close, focus trap + restore, stacking).
  - **RF**: 006 FR-30..FR-38; 005 FR-50; Edge cases "empty items", "modal stacking".
  - **Done when**: 006 TS-13..TS-17 pass.

- [ ] **Task 7: Core services — api, local-storage, auth + JWT interceptor (~30 min)**
  - **Description**: Implement `api.service` wrapping every `openapi.yaml` operation (login, createList, joinList, getActiveList, closeInvitations, addNames, finishAddition, completeSelection, getSelection, adoptFadedName, submitVote, getResults), `local-storage.service` (namespaced `list_{id}_*` keys, restore/clear with success/error policy), `auth.service` (token storage, re-auth trigger) and the `Authorization: Bearer` HTTP interceptor.
  - **RF**: 005 FR-1..FR-5, FR-24, FR-43, FR-44, FR-45, FR-46; 005 NFR-3, NFR-4, NFR-7.
  - **Done when**: `HttpTestingController`-based tests pass for each endpoint call, token attach, and cache clear/retain semantics.

- [ ] **Task 8: Auth feature — login screen (~20 min)**
  - **Description**: Compose login form (`ui-input-field`s + `ui-button`) with Angular Reactive Forms; on 401 show inline "Usuario o contraseña incorrectos" without clearing the form; store JWT and route to dashboard.
  - **RF**: 005 FR-1, FR-2, FR-3.
  - **Done when**: 005 TS-1, TS-2 pass.

- [ ] **Task 9: Dashboard feature (~30 min)**
  - **Description**: Compose dashboard from `ui-list-card` / empty state (`ui-button` "Crear lista nueva" + "Unirse con código"), create/join modals using `ui-modal`, 5-minute in-memory cache and re-fetch on visit, reload after phase completion, inline backend error display.
  - **RF**: 005 FR-6..FR-17.
  - **Done when**: 005 TS-3..TS-8, TS-28 pass.

- [ ] **Task 10: Suggestion feature (local-first sync) (~30 min)**
  - **Description**: Compose suggestion panel (`ui-name-input-row`, list with `ui-icon-button` delete, `ui-validation-message`), character validation ([letters/accents, spaces, hyphens]), case/accent-insensitive duplicate check, restore from localStorage on entry, "Terminar Fase" guard (≥1 name), two-step sync (`addNames` → `finishAddition`), clear-on-success / retain-on-error.
  - **RF**: 005 FR-18..FR-26; Edge cases invalid chars, duplicates, empty finish, backend unreachable.
  - **Done when**: 005 TS-9..TS-15 pass.

- [ ] **Task 11: Selection feature (~20 min)**
  - **Description**: Compose selection view (three sections common/faded/own via `ui-list-card` or list rows), adopt faded name (`POST adopt` + refresh), complete-selection with inline errors.
  - **RF**: 005 FR-27..FR-31.
  - **Done when**: 005 TS-16..TS-18 pass.

- [ ] **Task 12: Voting feature (round-based) (~30 min)**
  - **Description**: Compose `ui-draggable-ranking-list` + `ui-round-indicator`, restore local ranking per round, discard stale rankings (names not in `currentPool`), submit via `POST vote` (roundNumber + rankings), handle 409 (retain + error), 400/422 (red border + "Voto no válido"), 401 (re-auth modal then retry).
  - **RF**: 005 FR-32..FR-39; Edge cases stale ranking, session expiry.
  - **Done when**: 005 TS-19..TS-23, TS-26, TS-29 pass.

- [ ] **Task 13: Results feature (~15 min)**
  - **Description**: Compose results screen from data of `GET results`, "resultados siendo procesados" + retry button on 409, always fresh fetch (no cache).
  - **RF**: 005 FR-40..FR-42.
  - **Done when**: 005 TS-24, TS-25 pass.

- [ ] **Task 14: Full verification (~20 min)**
  - **Description**: Run `npm test` and `npm run lint`; verify 100% green and zero warnings; confirm every Spec 005/006 test scenario (TS-1..TS-29 for 005, TS-1..TS-17 for 006) is exercised.
  - **RF**: Constitution P3.2; AGENTS.md finishing rules.
  - **Done when**: Frontend suites pass 100%, zero lint warnings, and each scenario maps to a passing test.
