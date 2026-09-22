# Spec 005 — Frontend UI

## Context & Objectives
This specification defines the Angular 17 frontend implementation for the Collaborative Name Decider. The frontend is a fully decoupled presentation layer that communicates with the backend exclusively through REST API contracts defined in `specs/openapi.yaml` and Specs 001–004.

The architecture follows a **local-first strategy** during the suggestion and voting phases: all user interactions are buffered locally and synchronized with the backend only when the user explicitly completes a phase. This ensures offline resilience, delayed authentication handling, and a smooth collaborative experience.

A user can belong to **multiple lists at the same time**. The dashboard retrieves all the user's lists (`GET /api/v1/lists`), including completed lists whose results can still be consulted. Every authenticated screen shows a shared app bar with a "Cerrar sesión" action.

The UI is written entirely in Spanish. All code, variable/function names, commit messages, comments, and documentation are in English.

## Users / Actors
* **Participant**: An authenticated user who interacts with the frontend to create, join, and manage name-deciding lists.

## User Stories
* **US-1**: As a Participant, I want to log in so I can access my lists.
* **US-2**: As a Participant, I want to see all my lists and their current phases on a dashboard.
* **US-3**: As a Participant, I want to create a new list or join an existing one from the dashboard at any time.
* **US-4**: As a Participant, I want to enter the suggestion phase, add names locally, and validate them in real time.
* **US-5**: As a Participant, I want to complete the suggestion phase and send my names to the backend.
* **US-6**: As a Participant, I want to see the selection view (common names and faded suggestions) and adopt faded names.
* **US-7**: As a Participant, I want to vote by ranking names using drag-and-drop across multiple rounds.
* **US-8**: As a Participant, I want to see the final results once voting is complete.
* **US-9**: As a Participant, I want my local progress preserved in localStorage so I don't lose work if I disconnect or my session expires.
* **US-10**: As a new user, I want to register an account from the login screen, so that I can authenticate and use the application.
* **US-11**: As a Participant, I want to close my session from any authenticated screen.

---

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Authentication

*   **FR-1**: WHEN the Participant navigates to the application, THE SYSTEM MUST display a login screen with username and password fields.
*   **FR-2**: WHEN the Participant submits valid credentials (`POST /api/v1/auth/login`), THE SYSTEM MUST store the JWT in localStorage and redirect to the home dashboard.
*   **FR-3**: IF the credentials are invalid (401 response), THEN THE SYSTEM MUST display an inline error message ("Usuario o contraseña incorrectos") without clearing the form.
*   **FR-4**: IF the Participant's JWT expires while navigating the application, THEN THE SYSTEM MUST allow continued local editing and prompt for re-authentication only when the Participant clicks a phase-completion button ("Terminar Fase" / "Terminar Votación").
*   **FR-5**: WHEN the Participant re-authenticates after an expired session, THE SYSTEM MUST refresh the JWT in localStorage and retry the pending backend operation with the new token.

### 2. Home Dashboard

*   **FR-6**: WHEN the Participant is authenticated, THE SYSTEM MUST call `GET /api/v1/lists` and display all returned lists.
*   **FR-7**: FOR EACH returned list, THE SYSTEM MUST display a card showing the list name, the current phase (translated to Spanish), and the member count.
*   **FR-8**: IF `GET /api/v1/lists` returns an empty array (no lists), THEN THE SYSTEM MUST display an empty state with a message ("No tienes ninguna lista") and the two actions "Crear lista nueva" and "Unirse con código".
*   **FR-9**: WHEN the Participant clicks on a list card, THE SYSTEM MUST navigate to the view corresponding to that list's current phase (`/lists/{id}/suggestion`, `/lists/{id}/selection`, `/lists/{id}/vote`, or `/lists/{id}/results`).
*   **FR-10**: WHEN the Participant clicks "Crear lista nueva", THE SYSTEM MUST display a modal or inline form with a list name field and submit `POST /api/v1/lists`.
*   **FR-11**: WHEN a list creation succeeds (201), THE SYSTEM MUST navigate to the suggestion phase for the new list.
*   **FR-12**: IF a list creation fails (400), THEN THE SYSTEM MUST display the backend error message inline without navigating away.
*   **FR-13**: WHEN the Participant clicks "Unirse con código", THE SYSTEM MUST display a modal with a 6-character code field and submit `POST /api/v1/lists/join`.
*   **FR-14**: WHEN a join succeeds (200), THE SYSTEM MUST navigate to the suggestion phase for the joined list.
*   **FR-15**: IF a join fails (400/404), THEN THE SYSTEM MUST display the backend error message inline.
*   **FR-16**: THE SYSTEM MUST cache the dashboard data in memory. IF more than 5 minutes have elapsed since the last fetch, THEN THE SYSTEM MUST re-fetch from the API on the next dashboard visit.
*   **FR-17**: WHEN the Participant completes a phase and is redirected to the dashboard, THE SYSTEM MUST re-fetch the list state from the API and display the updated phases.
*   **FR-18**: THE SYSTEM MUST always render the "Crear lista nueva" and "Unirse con código" actions on the dashboard, both when the Participant has lists and when the Participant has none.

### 3. Suggestion Phase (Local-First, then Sync)

*   **FR-19**: WHEN the Participant enters the suggestion phase for a list, THE SYSTEM MUST restore any previously saved local suggestions from localStorage for that list.
*   **FR-20**: WHEN the Participant types a name in the text input field, THE SYSTEM MUST validate that the input contains only letters (including accented), spaces, and hyphens. IF the input contains invalid characters, THEN THE SYSTEM MUST prevent the addition and display an inline validation message ("Solo se permiten letras, espacios y guiones").
*   **FR-21**: WHEN the Participant submits a name (via the send button or the Enter key), THE SYSTEM MUST check for duplicates against the local list. IF the name already exists (case-insensitive, accent-insensitive), THEN THE SYSTEM MUST prevent the addition and display an inline duplicate warning ("Este nombre ya está en la lista").
*   **FR-22**: WHEN a valid, non-duplicate name is submitted, THE SYSTEM MUST add it to the local suggestion list and persist the updated list to localStorage.
*   **FR-23**: WHEN the Participant clicks the delete icon (X) next to a suggestion, THE SYSTEM MUST remove it from the local list and persist the change to localStorage.
*   **FR-24**: IF the Participant has zero suggestions when clicking "Terminar Fase", THEN THE SYSTEM MUST prevent the phase completion and display a message ("Debes añadir al menos un nombre").
*   **FR-25**: WHEN the Participant clicks "Terminar Fase" with at least one suggestion, THE SYSTEM MUST submit the suggestions to `POST /api/v1/lists/{id}/names` with the full names array, then call `POST /api/v1/lists/{id}/finish-addition`.
*   **FR-26**: IF the names submission succeeds (200) AND the finish-addition succeeds (200), THEN THE SYSTEM MUST clear the localStorage entry for that list and navigate back to the home dashboard.
*   **FR-27**: IF the names submission or finish-addition fails (400/401/404/409), THEN THE SYSTEM MUST display the error message ("Ha habido un error, inténtelo de nuevo") and retain the local suggestions in localStorage for retry.

### 4. Selection Phase

*   **FR-28**: WHEN the list is in SELECTION phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}/selection` and display three sections: "Nombres comunes" (commonNames), "Sugerencias" (fadedSuggestions), and "Mis nombres" (myNames).
*   **FR-29**: WHEN the Participant clicks on a faded suggestion, THE SYSTEM MUST call `POST /api/v1/lists/{id}/selection/adopt` with the adopted name. IF the adoption succeeds (200), THEN THE SYSTEM MUST refresh the selection view by re-fetching `GET /api/v1/lists/{id}/selection`.
*   **FR-30**: IF the adoption fails (400/404), THEN THE SYSTEM MUST display the backend error message inline.
*   **FR-31**: WHEN the Participant clicks "Completar selección", THE SYSTEM MUST call `POST /api/v1/lists/{id}/complete-selection`. IF it succeeds (200), THEN THE SYSTEM MUST navigate back to the dashboard.
*   **FR-32**: IF complete-selection fails (400), THEN THE SYSTEM MUST display the error message and remain on the selection view.

### 5. Voting Phase (Local-First, Round-Based)

*   **FR-33**: WHEN the list is in VOTING phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}` to obtain `currentRound`, `totalRounds`, and `currentPool`. THE SYSTEM MUST display the names from `currentPool` in a rankable order using drag-and-drop, along with a round indicator ("Ronda X de Y").
*   **FR-34**: IF there is a saved ranking in localStorage for the current round, THEN THE SYSTEM MUST restore it. IF the saved ranking contains names no longer in `currentPool` (eliminated in a previous round), THEN THE SYSTEM MUST discard the stale ranking and display the current pool in default order.
*   **FR-35**: WHEN the Participant reorders names via drag-and-drop, THE SYSTEM MUST persist the new order to localStorage in real time for the current round.
*   **FR-36**: WHEN the Participant clicks "Enviar voto", THE SYSTEM MUST submit the ranked order to `POST /api/v1/lists/{id}/vote` with the current `roundNumber` and the `rankings` array (names from most to least preferred).
*   **FR-37**: IF the vote submission succeeds (200), THEN THE SYSTEM MUST clear the localStorage entry for that round and navigate back to the home dashboard.
*   **FR-38**: IF the vote submission fails with 409 (Conflict — e.g., stale round or already voted), THEN THE SYSTEM MUST display the error message ("Ha habido un error, inténtelo de nuevo") and retain the ranking in localStorage for retry.
*   **FR-39**: IF the vote submission fails with 400/422 (invalid ranking — e.g., missing names, wrong count), THEN THE SYSTEM MUST mark the ranking container with a red border and display an error message ("Voto no válido. Revisa el orden de los nombres").
*   **FR-40**: IF the vote submission fails with 401 (expired token), THEN THE SYSTEM MUST prompt for re-authentication and retry the submission with the new token.

### 6. Results Screen

*   **FR-41**: WHEN the list is in COMPLETED phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}/results` and display the ranked results (top-3 with scores).
*   **FR-42**: IF the results endpoint returns 409 (results not ready), THEN THE SYSTEM MUST display a message ("Los resultados están siendo procesados") and offer a retry button.
*   **FR-43**: THE SYSTEM MUST NOT cache results. Each visit to the results screen triggers a fresh API call.

### 7. Cache and Local State Management

*   **FR-44**: THE SYSTEM MUST manage localStorage entries per list, using the list ID as the namespace key (e.g., `list_42_suggestions`, `list_42_vote_round_1`).
*   **FR-45**: WHEN a phase is successfully completed (backend returns 200), THE SYSTEM MUST clear ALL localStorage entries for that list's phase.
*   **FR-46**: WHEN a phase completion fails, THE SYSTEM MUST retain ALL localStorage entries and display the error message ("Ha habido un error, inténtelo de nuevo").
*   **FR-47**: IF localStorage is unavailable or quota is exceeded, THEN THE SYSTEM MUST display a warning ("Tu progreso no se guardará localmente. No cierres la página.") and continue allowing interaction without persistence.
*   **FR-48**: WHEN the Participant navigates to a list detail view (suggestion, selection, or voting), THE SYSTEM MUST call the corresponding API endpoint to obtain the current state. The frontend never assumes the API state matches localStorage.

### 8. Component Architecture

*   **FR-49**: THE SYSTEM MUST consume the `ui-kit` library (defined in `specs/006-ui-kit.md`) for all reusable UI elements. The main application must NOT re-implement atoms, molecules, or organisms that already exist in `ui-kit`.
*   **FR-50**: THE SYSTEM MUST use Angular 17 Standalone Components throughout, with no NgModules.
*   **FR-51**: THE SYSTEM MUST use the `ui-draggable-ranking-list` organism from `ui-kit` (backed by Angular CDK) for drag-and-drop functionality in the voting phase.
*   **FR-52**: THE SYSTEM MUST use Angular Reactive Forms for all form handling (login, registration, name input, code input, list creation).

### 9. Self-Service Registration

*   **FR-53**: WHEN an unauthenticated Participant opens the login screen, THE SYSTEM MUST display a visible "Crear cuenta" action that navigates to the registration screen.
*   **FR-54**: WHEN the Participant opens the registration screen, THE SYSTEM MUST display a form with username and password fields using the `ui-input-field` atom, plus a submit button ("Registrarse") and a back-to-login action.
*   **FR-55**: IF the username field is blank or contains only whitespace, THEN THE SYSTEM MUST display the inline validation message ("El usuario es obligatorio") and prevent submission.
*   **FR-56**: IF the password does not meet the complexity requirement (minimum 8 characters, at least one uppercase letter, at least one digit), THEN THE SYSTEM MUST display the corresponding inline validation message(s) ("Mínimo 8 caracteres", "Debe incluir una letra mayúscula", "Debe incluir un número") and prevent submission without sending any request.
*   **FR-57**: WHEN the Participant submits valid registration data, THE SYSTEM MUST call `POST /api/v1/auth/register` with a JSON body `{ "username": string, "password": string }`.
*   **FR-58**: IF the registration succeeds (201 Created), THEN THE SYSTEM MUST redirect to the login screen and display a success message ("Cuenta creada correctamente. Inicia sesión."). THE SYSTEM MUST NOT store any token as a result of registration.
*   **FR-59**: IF the registration fails with 409 Conflict (username already exists), THEN THE SYSTEM MUST display the inline error message ("El usuario ya existe") and retain the entered values for correction.
*   **FR-60**: IF the registration fails with 400 Bad Request (e.g., password rejected server-side), THEN THE SYSTEM MUST display the inline error message ("Verifica los datos introducidos") and retain the entered values.
*   **FR-61**: IF the registration fails with a network error or a 5xx response, THEN THE SYSTEM MUST display the inline error message ("Ha habido un error, inténtelo de nuevo") and retain the entered values.
*   **FR-62**: IF an authenticated Participant navigates to the registration screen, THEN THE SYSTEM MUST redirect them to the home dashboard.

### 10. App Bar & Session Management

*   **FR-63**: WHEN the Participant is authenticated and views the dashboard or any list screen, THE SYSTEM MUST render the shared `ui-app-bar` at the top with the application title and a "Cerrar sesión" button.
*   **FR-64**: WHEN the Participant clicks the application title in the `ui-app-bar`, THE SYSTEM MUST navigate to the home dashboard (`/`).
*   **FR-65**: WHEN the Participant clicks "Cerrar sesión", THE SYSTEM MUST clear the stored JWT, invalidate the in-memory dashboard cache, clear ALL localStorage entries for list progress (keys prefixed with `list_`), and navigate to the login screen. The Participant MUST NOT be able to reach authenticated screens after logging out.
*   **FR-66**: THE SYSTEM MUST NOT render the `ui-app-bar` on the login or registration screens.

---

## Non-Functional Requirements
*   **NFR-1 (Architecture)**: Frontend is fully decoupled from the backend. All communication is via REST API contracts defined in `specs/openapi.yaml`. No business logic, data normalization, or phase-transition logic belongs in the frontend.
*   **NFR-2 (Language)**: UI text visible to end users is in Spanish. All code, variable/function names, commit messages, comments, and documentation are in English.
*   **NFR-3 (State Management)**: Local state during suggestion and voting phases is managed via a dedicated Angular service (`LocalStorageService`) that wraps localStorage operations, namespaced per list ID and phase.
*   **NFR-4 (Security)**: JWT tokens are stored in localStorage. All API requests include the token via an HTTP interceptor. On logout the token and ALL cached/namespaced state (in-memory dashboard cache and `list_*` localStorage entries) are cleared to prevent cross-session data leakage.
*   **NFR-5 (Componentization)**: All reusable UI elements are provided by the `ui-kit` library (atoms, molecules, organisms per Spec 006). The main application contains page-level presentational logic and business services only.
*   **NFR-6 (Performance)**: The application must load the initial dashboard within 2 seconds on a standard broadband connection.
*   **NFR-7 (API Alignment)**: Every frontend API call must map directly to an operation defined in `specs/openapi.yaml`. The frontend MUST NOT invent or assume endpoints that do not exist in the contract.
*   **NFR-8 (Multi-List Support)**: All screen state must be resolved from the list id in the route (`listId`), never from a global "active list". Navigating between lists must not leak state between them.

---

## Edge Cases
*   **Session Expiry During Editing**: The Participant can continue editing locally. The JWT is re-requested only at phase completion time (FR-4, FR-40).
*   **localStorage Full or Unavailable**: Degrades gracefully with a warning. Progress does not survive page reloads (FR-47).
*   **Concurrent List State Changes**: IF the backend returns 409 (Conflict) at any phase completion, THEN the system displays the error message and retains local state for retry (FR-38, FR-46).
*   **Empty Suggestion List at Phase Completion**: Rejected on the frontend before any API call (FR-24).
*   **Invalid Characters in Suggestion Input**: Rejected on the frontend before adding to the local list (FR-20).
*   **Duplicate Name in Local List**: Prevented on the frontend before adding to the local list (FR-21).
*   **Duplicate Username at Registration**: IF the backend returns 409, THEN the system shows "El usuario ya existe" and retains the form for correction (FR-59).
*   **Weak Password or Blank Fields at Registration**: Rejected client-side before any request is sent, with field-level Spanish messages (FR-55, FR-56).
*   **Backend Unreachable at Registration**: The system shows a connection error and retains the entered values (FR-61).
*   **Backend Unreachable at Phase Completion**: The system displays a connection error and retains local state in localStorage (FR-46).
*   **Stale Vote Ranking**: IF localStorage contains a ranking with names no longer in the current round's pool, THEN the system discards the stale data and resets to default order (FR-34).
*   **Multitab Editing**: IF the user opens the same list in multiple browser tabs, THEN localStorage changes will sync between tabs. This is acceptable behavior since each phase completion sends the full local state.
*   **Phase Changed While Offline**: IF the backend phase has changed (e.g., transitioned) while the user was offline, THEN the phase completion call will fail with 400/409 and the user will see the error message and be redirected to the dashboard.
*   **List Owner Closes Invitations While Offline**: IF the owner closes invitations while the user is offline, the user's local editing is unaffected. The next API call will reflect the updated state.
*   **Results Not Ready**: IF the results endpoint returns 409, the system shows a processing message with a retry button (FR-42).
*   **User in Multiple Lists**: The dashboard shows one card per list; the "Crear lista nueva" and "Unirse con código" actions remain available, and each list is opened by its own id (FR-9, FR-18, NFR-8).

---

## Initial Test Scenarios (TDD Requirement)
*   **TS-1**: Login with valid credentials → JWT stored, redirected to dashboard.
*   **TS-2**: Login with invalid credentials → inline error displayed, form preserved.
*   **TS-3**: Dashboard with lists → one card per list displayed with correct phase.
*   **TS-4**: Dashboard with no lists → empty state with "Crear lista" and "Unirse" actions.
*   **TS-5**: Create list successfully → navigated to suggestion phase.
*   **TS-6**: Create list with API failure (e.g., blank name → 400) → error message displayed, no navigation.
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
*   **TS-30**: Login screen shows "Crear cuenta" action → navigates to the registration screen.
*   **TS-31**: Register with valid data → 201, redirected to login with success message, no token stored.
*   **TS-32**: Register with existing username → 409, inline error displayed, form retained.
*   **TS-33**: Register with weak password → client-side field errors shown, no request sent.
*   **TS-34**: Register with blank username/password → client-side field errors shown, no request sent.
*   **TS-35**: Register with network error or 5xx → generic error displayed, form retained.
*   **TS-36**: Authenticated user opens registration screen → redirected to home dashboard.
*   **TS-37**: User in multiple lists → dashboard shows one card per list and the "Crear lista"/"Unirse" actions remain visible.
*   **TS-38**: Authenticated screen shows the app bar; login and registration screens do not.
*   **TS-39**: Click "Cerrar sesión" → JWT cleared, dashboard cache and `list_*` localStorage entries cleared, redirected to `/login`.
*   **TS-40**: Create or join a second list while already in another list → new list card appears on the dashboard without losing the first.

---

## Out of Scope
*   Real-time collaborative updates (WebSockets) during suggestion or voting phases.
*   Automatic matching or grouping of similar names (backend concern, Spec 003).
*   Elimination round UI logic (round transitions are automated by the backend, Spec 004). The frontend only reacts to the `currentRound` and `totalRounds` values returned by the API.
*   Token refresh logic (Spec 001).
*   Responsive/mobile-first layout optimization (desktop-first for MVP).
*   Accessibility (WCAG) compliance beyond basic semantic HTML (future iteration).
*   Removing a user from a list (voluntary leave or membership removal).

---

## Completion Criteria
*   Angular 17 project scaffolded with standalone components, Angular CDK (`@angular/cdk/drag-drop`), and routing configured.
*   `ui-kit` library consumed by the main application (see Spec 006 for library completion criteria).
*   Login flow implemented with JWT storage and HTTP interceptor.
*   Registration flow implemented: "Crear cuenta" action on the login screen, registration screen (username + password), client-side validation, `POST /api/v1/auth/register` call, and success/error handling (FR-53..FR-62).
*   Home dashboard displaying all the user's lists (or the empty state) with correct phase mapping, and create/join actions always available (FR-6..FR-18).
*   Shared app bar with "Cerrar sesión" on all authenticated screens; logout clears token, in-memory cache, and `list_*` localStorage entries (FR-63..FR-66).
*   Suggestion phase with local-first persistence, character validation, duplicate detection, two-step sync (`addNames` → `finishAddition`), and error recovery.
*   Selection phase with common/faded/own names display and adoption flow.
*   Voting phase with drag-and-drop ranking, round-based submission, local-first persistence, and invalid-vote feedback (red border).
*   Results screen displaying final rankings.
*   Cache management verified: clear on success, retain on error, 5-minute dashboard refresh.
*   Full test suite passing (`npm test`) with zero lint warnings (`npm run lint`).