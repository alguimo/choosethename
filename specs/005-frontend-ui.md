# Spec 005 — Frontend UI

## Context & Objectives
This specification defines the Angular 17 frontend implementation for the Collaborative Name Decider. The frontend is a fully decoupled presentation layer that communicates with the backend exclusively through REST API contracts defined in `specs/openapi.yaml` and Specs 001–004.

The architecture follows a **local-first strategy** during the suggestion and voting phases: all user interactions are buffered locally and synchronized with the backend only when the user explicitly completes a phase. This ensures offline resilience, delayed authentication handling, and a smooth collaborative experience.

**Known API constraint**: The backend exposes `GET /api/v1/lists/active` which returns a single active list per user, not a collection. The frontend dashboard is designed around this constraint.

The UI is written entirely in Spanish. All code, variable/function names, commit messages, comments, and documentation are in English.

## Users / Actors
* **Participant**: An authenticated user who interacts with the frontend to create, join, and manage name-deciding lists.

## User Stories
* **US-1**: As a Participant, I want to log in so I can access my active list.
* **US-2**: As a Participant, I want to see my active list and its current phase on a dashboard.
* **US-3**: As a Participant, I want to create a new list or join an existing one from the dashboard.
* **US-4**: As a Participant, I want to enter the suggestion phase, add names locally, and validate them in real time.
* **US-5**: As a Participant, I want to complete the suggestion phase and send my names to the backend.
* **US-6**: As a Participant, I want to see the selection view (common names and faded suggestions) and adopt faded names.
* **US-7**: As a Participant, I want to vote by ranking names using drag-and-drop across multiple rounds.
* **US-8**: As a Participant, I want to see the final results once voting is complete.
* **US-9**: As a Participant, I want my local progress preserved in localStorage so I don't lose work if I disconnect or my session expires.

---

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Authentication

*   **FR-1**: WHEN the Participant navigates to the application, THE SYSTEM MUST display a login screen with username and password fields.
*   **FR-2**: WHEN the Participant submits valid credentials (`POST /api/v1/auth/login`), THE SYSTEM MUST store the JWT in localStorage and redirect to the home dashboard.
*   **FR-3**: IF the credentials are invalid (401 response), THEN THE SYSTEM MUST display an inline error message ("Usuario o contraseña incorrectos") without clearing the form.
*   **FR-4**: IF the Participant's JWT expires while navigating the application, THEN THE SYSTEM MUST allow continued local editing and prompt for re-authentication only when the Participant clicks a phase-completion button ("Terminar Fase" / "Terminar Votación").
*   **FR-5**: WHEN the Participant re-authenticates after an expired session, THE SYSTEM MUST refresh the JWT in localStorage and retry the pending backend operation with the new token.

### 2. Home Dashboard

*   **FR-6**: WHEN the Participant is authenticated, THE SYSTEM MUST call `GET /api/v1/lists/active` and display the result.
*   **FR-7**: IF the API returns a list (200 OK), THEN THE SYSTEM MUST display a card showing the list name, the current phase (translated to Spanish), and the member count.
*   **FR-8**: IF the API returns 404 (no active list), THEN THE SYSTEM MUST display an empty state with two actions: "Crear lista nueva" and "Unirse con código".
*   **FR-9**: WHEN the Participant clicks on the active list card, THE SYSTEM MUST call `GET /api/v1/lists/{id}` (or re-fetch `GET /api/v1/lists/active`) and navigate to the view corresponding to the list's current phase.
*   **FR-10**: WHEN the Participant clicks "Crear lista nueva", THE SYSTEM MUST display a modal or inline form with a list name field and submit `POST /api/v1/lists`.
*   **FR-11**: WHEN a list creation succeeds (201), THE SYSTEM MUST navigate to the suggestion phase for the new list.
*   **FR-12**: IF a list creation fails (400 — e.g., user already belongs to an active list), THEN THE SYSTEM MUST display the backend error message inline without navigating away.
*   **FR-13**: WHEN the Participant clicks "Unirse con código", THE SYSTEM MUST display a modal with a 6-character code field and submit `POST /api/v1/lists/join`.
*   **FR-14**: WHEN a join succeeds (200), THE SYSTEM MUST navigate to the suggestion phase for the joined list.
*   **FR-15**: IF a join fails (400/404), THEN THE SYSTEM MUST display the backend error message inline.
*   **FR-16**: THE SYSTEM MUST cache the dashboard data in memory. IF more than 5 minutes have elapsed since the last fetch, THEN THE SYSTEM MUST re-fetch from the API on the next dashboard visit.
*   **FR-17**: WHEN the Participant completes a phase and is redirected to the dashboard, THE SYSTEM MUST re-fetch the list state from the API and display the updated phase.

### 3. Suggestion Phase (Local-First, then Sync)

*   **FR-18**: WHEN the Participant enters the suggestion phase for a list, THE SYSTEM MUST restore any previously saved local suggestions from localStorage for that list.
*   **FR-19**: WHEN the Participant types a name in the text input field, THE SYSTEM MUST validate that the input contains only letters (including accented), spaces, and hyphens. IF the input contains invalid characters, THEN THE SYSTEM MUST prevent the addition and display an inline validation message ("Solo se permiten letras, espacios y guiones").
*   **FR-20**: WHEN the Participant submits a name (via the send button or the Enter key), THE SYSTEM MUST check for duplicates against the local list. IF the name already exists (case-insensitive, accent-insensitive), THEN THE SYSTEM MUST prevent the addition and display an inline duplicate warning ("Este nombre ya está en la lista").
*   **FR-21**: WHEN a valid, non-duplicate name is submitted, THE SYSTEM MUST add it to the local suggestion list and persist the updated list to localStorage.
*   **FR-22**: WHEN the Participant clicks the delete icon (X) next to a suggestion, THE SYSTEM MUST remove it from the local list and persist the change to localStorage.
*   **FR-23**: IF the Participant has zero suggestions when clicking "Terminar Fase", THEN THE SYSTEM MUST prevent the phase completion and display a message ("Debes añadir al menos un nombre").
*   **FR-24**: WHEN the Participant clicks "Terminar Fase" with at least one suggestion, THE SYSTEM MUST submit the suggestions to `POST /api/v1/lists/{id}/names` with the full names array, then call `POST /api/v1/lists/{id}/finish-addition`.
*   **FR-25**: IF the names submission succeeds (200) AND the finish-addition succeeds (200), THEN THE SYSTEM MUST clear the localStorage entry for that list and navigate back to the home dashboard.
*   **FR-26**: IF the names submission or finish-addition fails (400/401/404/409), THEN THE SYSTEM MUST display the error message ("Ha habido un error, inténtelo de nuevo") and retain the local suggestions in localStorage for retry.

### 4. Selection Phase

*   **FR-27**: WHEN the list is in SELECTION phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}/selection` and display three sections: "Nombres comunes" (commonNames), "Sugerencias" (fadedSuggestions), and "Mis nombres" (myNames).
*   **FR-28**: WHEN the Participant clicks on a faded suggestion, THE SYSTEM MUST call `POST /api/v1/lists/{id}/selection/adopt` with the adopted name. IF the adoption succeeds (200), THEN THE SYSTEM MUST refresh the selection view by re-fetching `GET /api/v1/lists/{id}/selection`.
*   **FR-29**: IF the adoption fails (400/404), THEN THE SYSTEM MUST display the backend error message inline.
*   **FR-30**: WHEN the Participant clicks "Completar selección", THE SYSTEM MUST call `POST /api/v1/lists/{id}/complete-selection`. IF it succeeds (200), THEN THE SYSTEM MUST navigate back to the dashboard.
*   **FR-31**: IF complete-selection fails (400), THEN THE SYSTEM MUST display the error message and remain on the selection view.

### 5. Voting Phase (Local-First, Round-Based)

*   **FR-32**: WHEN the list is in VOTING phase, THE SYSTEM MUST call `GET /api/v1/lists/active` to obtain `currentRound`, `totalRounds`, and `currentPool`. THE SYSTEM MUST display the names from `currentPool` in a rankable order using drag-and-drop, along with a round indicator ("Ronda X de Y").
*   **FR-33**: IF there is a saved ranking in localStorage for the current round, THEN THE SYSTEM MUST restore it. IF the saved ranking contains names no longer in `currentPool` (eliminated in a previous round), THEN THE SYSTEM MUST discard the stale ranking and display the current pool in default order.
*   **FR-34**: WHEN the Participant reorders names via drag-and-drop, THE SYSTEM MUST persist the new order to localStorage in real time for the current round.
*   **FR-35**: WHEN the Participant clicks "Enviar voto", THE SYSTEM MUST submit the ranked order to `POST /api/v1/lists/{id}/vote` with the current `roundNumber` and the `rankings` array (names from most to least preferred).
*   **FR-36**: IF the vote submission succeeds (200), THEN THE SYSTEM MUST clear the localStorage entry for that round and navigate back to the home dashboard.
*   **FR-37**: IF the vote submission fails with 409 (Conflict — e.g., stale round or already voted), THEN THE SYSTEM MUST display the error message ("Ha habido un error, inténtelo de nuevo") and retain the ranking in localStorage for retry.
*   **FR-38**: IF the vote submission fails with 400/422 (invalid ranking — e.g., missing names, wrong count), THEN THE SYSTEM MUST mark the ranking container with a red border and display an error message ("Voto no válido. Revisa el orden de los nombres").
*   **FR-39**: IF the vote submission fails with 401 (expired token), THEN THE SYSTEM MUST prompt for re-authentication and retry the submission with the new token.

### 6. Results Screen

*   **FR-40**: WHEN the list is in COMPLETED phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}/results` and display the ranked results (top-3 with scores).
*   **FR-41**: IF the results endpoint returns 409 (results not ready), THEN THE SYSTEM MUST display a message ("Los resultados están siendo procesados") and offer a retry button.
*   **FR-42**: THE SYSTEM MUST NOT cache results. Each visit to the results screen triggers a fresh API call.

### 7. Cache and Local State Management

*   **FR-43**: THE SYSTEM MUST manage localStorage entries per list, using the list ID as the namespace key (e.g., `list_42_suggestions`, `list_42_vote_round_1`).
*   **FR-44**: WHEN a phase is successfully completed (backend returns 200), THE SYSTEM MUST clear ALL localStorage entries for that list's phase.
*   **FR-45**: WHEN a phase completion fails, THE SYSTEM MUST retain ALL localStorage entries and display the error message ("Ha habido un error, inténtelo de nuevo").
*   **FR-46**: IF localStorage is unavailable or quota is exceeded, THEN THE SYSTEM MUST display a warning ("Tu progreso no se guardará localmente. No cierres la página.") and continue allowing interaction without persistence.
*   **FR-47**: WHEN the Participant navigates to a list detail view (suggestion, selection, or voting), THE SYSTEM MUST call the corresponding API endpoint to obtain the current state. The frontend never assumes the API state matches localStorage.

### 8. Component Architecture

*   **FR-48**: THE SYSTEM MUST consume the `ui-kit` library (defined in `specs/006-ui-kit.md`) for all reusable UI elements. The main application must NOT re-implement atoms, molecules, or organisms that already exist in `ui-kit`.
*   **FR-49**: THE SYSTEM MUST use Angular 17 Standalone Components throughout, with no NgModules.
*   **FR-50**: THE SYSTEM MUST use the `ui-draggable-ranking-list` organism from `ui-kit` (backed by Angular CDK) for drag-and-drop functionality in the voting phase.
*   **FR-51**: THE SYSTEM MUST use Angular Reactive Forms for all form handling (login, name input, code input, list creation).

---

## Non-Functional Requirements
*   **NFR-1 (Architecture)**: Frontend is fully decoupled from the backend. All communication is via REST API contracts defined in `specs/openapi.yaml`. No business logic, data normalization, or phase-transition logic belongs in the frontend.
*   **NFR-2 (Language)**: UI text visible to end users is in Spanish. All code, variable/function names, commit messages, comments, and documentation are in English.
*   **NFR-3 (State Management)**: Local state during suggestion and voting phases is managed via a dedicated Angular service (`LocalStorageService`) that wraps localStorage operations, namespaced per list ID and phase.
*   **NFR-4 (Security)**: JWT tokens are stored in localStorage. All API requests include the token via an HTTP interceptor. Tokens are cleared on logout or when the user explicitly logs out.
*   **NFR-5 (Componentization)**: All reusable UI elements are provided by the `ui-kit` library (atoms, molecules, organisms per Spec 006). The main application contains page-level presentational logic and business services only.
*   **NFR-6 (Performance)**: The application must load the initial dashboard within 2 seconds on a standard broadband connection.
*   **NFR-7 (API Alignment)**: Every frontend API call must map directly to an operation defined in `specs/openapi.yaml`. The frontend MUST NOT invent or assume endpoints that do not exist in the contract.

---

## Edge Cases
*   **Session Expiry During Editing**: The Participant can continue editing locally. The JWT is re-requested only at phase completion time (FR-4, FR-39).
*   **localStorage Full or Unavailable**: Degrades gracefully with a warning. Progress does not survive page reloads (FR-46).
*   **Concurrent List State Changes**: IF the backend returns 409 (Conflict) at any phase completion, THEN the system displays the error message and retains local state for retry (FR-37, FR-45).
*   **Empty Suggestion List at Phase Completion**: Rejected on the frontend before any API call (FR-23).
*   **Invalid Characters in Suggestion Input**: Rejected on the frontend before adding to the local list (FR-19).
*   **Duplicate Name in Local List**: Prevented on the frontend before adding to the local list (FR-20).
*   **Backend Unreachable at Phase Completion**: The system displays a connection error and retains local state in localStorage (FR-45).
*   **Stale Vote Ranking**: IF localStorage contains a ranking with names no longer in the current round's pool, THEN the system discards the stale data and resets to default order (FR-33).
*   **Multitab Editing**: IF the user opens the same list in multiple browser tabs, THEN localStorage changes will sync between tabs. This is acceptable behavior since each phase completion sends the full local state.
*   **Phase Changed While Offline**: IF the backend phase has changed (e.g., transitioned to EXPIRED) while the user was offline, THEN the phase completion call will fail with 400/409 and the user will see the error message and be redirected to the dashboard.
*   **List Owner Closes Invitations While Offline**: IF the owner closes invitations while the user is offline, the user's local editing is unaffected. The next API call will reflect the updated state.
*   **Results Not Ready**: IF the results endpoint returns 409, the system shows a processing message with a retry button (FR-41).

---

## Initial Test Scenarios (TDD Requirement)
*   **TS-1**: Login with valid credentials → JWT stored, redirected to dashboard.
*   **TS-2**: Login with invalid credentials → inline error displayed, form preserved.
*   **TS-3**: Dashboard with active list → list card displayed with correct phase.
*   **TS-4**: Dashboard with no active list → empty state with "Crear lista" and "Unirse" actions.
*   **TS-5**: Create list successfully → navigated to suggestion phase.
*   **TS-6**: Create list when user already has active list → error message displayed.
*   **TS-7**: Join list with valid code → navigated to suggestion phase.
*   **TS-8**: Join list with invalid/non-existent code → error message displayed.
*   **TS-9**: Suggestion input with invalid characters → prevented, inline validation shown.
*   **TS-10**: Suggestion input with duplicate name → prevented, duplicate warning shown.
*   **TS-11**: Add valid suggestion → persisted to localStorage, displayed in list.
*   **TS-12**: Delete suggestion → removed from localStorage, list updated.
*   **TS-13**: Finish addition with zero suggestions → prevented, message shown.
*   **TS-14**: Finish addition with suggestions → names sent, finish-addition called, localStorage cleared, redirected to dashboard.
*   **TS-15**: Finish addition with API failure → error message displayed, localStorage retained.
*   **TS-16**: Selection view loads → common, faded, and own names displayed.
*   **TS-17**: Adopt faded name → name moved to shared pool, view refreshed.
*   **TS-18**: Complete selection → navigated to dashboard.
*   **TS-19**: Voting view loads → names displayed in drag-and-drop order with round indicator.
*   **TS-20**: Drag-and-drop reorder → new order persisted to localStorage.
*   **TS-21**: Submit vote successfully → localStorage cleared, redirected to dashboard.
*   **TS-22**: Submit stale vote (409 Conflict) → error message displayed, ranking retained.
*   **TS-23**: Submit invalid vote (400/422) → red border on ranking, error message displayed.
*   **TS-24**: Results screen loads → top-3 names with scores displayed.
*   **TS-25**: Results not ready (409) → processing message with retry button.
*   **TS-26**: Session expiry during editing → local editing continues, re-auth prompted at phase completion.
*   **TS-27**: localStorage unavailable → warning displayed, interaction continues without persistence.
*   **TS-28**: Dashboard cache refresh → re-fetches from API after 5-minute threshold.
*   **TS-29**: Stale ranking in localStorage (names eliminated in previous round) → discarded, reset to current pool order.

---

## Out of Scope
*   Real-time collaborative updates (WebSockets) during suggestion or voting phases.
*   Automatic matching or grouping of similar names (backend concern, Spec 003).
*   Elimination round UI logic (round transitions are automated by the backend, Spec 004). The frontend only reacts to the `currentRound` and `totalRounds` values returned by the API.
*   User registration (admin-only, Spec 001).
*   Token refresh logic (Spec 001).
*   Responsive/mobile-first layout optimization (desktop-first for MVP).
*   Accessibility (WCAG) compliance beyond basic semantic HTML (future iteration).
*   Displaying multiple active lists per user (API returns a single active list via `GET /api/v1/lists/active`).

---

## Completion Criteria
*   Angular 17 project scaffolded with standalone components, Angular CDK (`@angular/cdk/drag-drop`), and routing configured.
*   `ui-kit` library consumed by the main application (see Spec 006 for library completion criteria).
*   Login flow implemented with JWT storage and HTTP interceptor.
*   Home dashboard displaying the active list (or empty state) with correct phase mapping.
*   Suggestion phase with local-first persistence, character validation, duplicate detection, two-step sync (`addNames` → `finishAddition`), and error recovery.
*   Selection phase with common/faded/own names display and adoption flow.
*   Voting phase with drag-and-drop ranking, round-based submission, local-first persistence, and invalid-vote feedback (red border).
*   Results screen displaying final rankings.
*   Cache management verified: clear on success, retain on error, 5-minute dashboard refresh.
*   Full test suite passing (`npm test`) with zero lint warnings (`npm run lint`).
