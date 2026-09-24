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
*   **FR-82**: WHEN the suggestion screen is rendered and the list's `invitationsOpen` is `true` with a non-empty `invitationCode`, THEN THE SYSTEM MUST render an "Invitar" action in the suggestion screen header that opens the shared invitation modal (`ui-invite-modal`) showing the code for that list. IF `invitationsOpen` is `false`, THEN THE SYSTEM MUST NOT render that action.
*   **FR-83**: IF the list is still in ADDITION phase but `ListResponse.myStepCompleted` is `true` (the Participant already finished their names and is waiting for the other members), THEN THE SYSTEM MUST render the suggestion screen in read-only mode: the name input, delete actions and "Terminar Fase" MUST be disabled, the Participant's submitted names MUST be fetched via `GET /api/v1/lists/{id}/names` for display, and a waiting message ("Esperando a que el resto complete la fase") MUST be displayed.

### 4. Selection Phase

*   **FR-28**: WHEN the list is in SELECTION phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}/selection` and display three sections: "Nombres comunes" (commonNames), "Sugerencias" (fadedSuggestions), and "Mis nombres" (myNames). Common names and faded suggestions flagged as adopted MUST be rendered as **selected/marked** (highlighted with a check indicator). Faded suggestions flagged as not adopted MUST be rendered grayed out and clickable to adopt.
*   **FR-29**: WHEN the Participant clicks on an unadopted faded suggestion, THE SYSTEM MUST call `POST /api/v1/lists/{id}/selection/adopt` with the adopted name. IF the adoption succeeds (200), THEN THE SYSTEM MUST refresh the selection view by re-fetching `GET /api/v1/lists/{id}/selection`; the adopted name remains in the "Sugerencias" section but is now rendered as selected/marked. Adoption is one-way and cannot be undone.
*   **FR-30**: IF the adoption fails (400/404), THEN THE SYSTEM MUST display the backend error message inline.
*   **FR-31**: WHEN the Participant clicks "Completar selección", THE SYSTEM MUST call `POST /api/v1/lists/{id}/complete-selection`. IF it succeeds (200), THEN THE SYSTEM MUST navigate back to the dashboard.
*   **FR-32**: IF complete-selection fails (400), THEN THE SYSTEM MUST display the error message and remain on the selection view.
*   **FR-85**: THE SYSTEM MUST render the "Mis nombres" section as a smaller, collapsible list (e.g., a `<details>` element with a "Mis nombres (n)" summary) so the Participant's own proposals do not look like the rest of the names.
*   **FR-86**: IF the list is still in SELECTION phase but `ListResponse.myStepCompleted` is `true` (the Participant already completed their selection and is waiting for the other members), THEN THE SYSTEM MUST render the selection view in read-only mode: adoption buttons and "Completar selección" MUST be disabled and a waiting message ("Esperando a que el resto complete la fase") MUST be displayed.

### 5. Voting Phase (Local-First, Round-Based)

*   **FR-33**: WHEN the list is in VOTING phase, THE SYSTEM MUST call `GET /api/v1/lists/{id}` to obtain `currentRound`, `totalRounds`, and `currentPool`. THE SYSTEM MUST display the names from `currentPool` in a rankable order using drag-and-drop, along with a round indicator ("Ronda X de Y").
*   **FR-34**: IF there is a saved ranking in localStorage for the current round, THEN THE SYSTEM MUST restore it. IF the saved ranking contains names no longer in `currentPool` (eliminated in a previous round), THEN THE SYSTEM MUST discard the stale ranking and display the current pool in default order.
*   **FR-35**: WHEN the Participant reorders names via drag-and-drop, THE SYSTEM MUST persist the new order to localStorage in real time for the current round.
*   **FR-36**: WHEN the Participant clicks "Enviar voto", THE SYSTEM MUST submit the ranked order to `POST /api/v1/lists/{id}/vote` with the current `roundNumber` and the `rankings` array (names from most to least preferred).
*   **FR-37**: IF the vote submission succeeds (200), THEN THE SYSTEM MUST clear the localStorage entry for that round and navigate back to the home dashboard.
*   **FR-38**: IF the vote submission fails with 409 (Conflict — e.g., stale round or already voted), THEN THE SYSTEM MUST display the error message ("Ha habido un error, inténtelo de nuevo") and retain the ranking in localStorage for retry.
*   **FR-39**: IF the vote submission fails with 400/422 (invalid ranking — e.g., missing names, wrong count), THEN THE SYSTEM MUST mark the ranking container with a red border and display an error message ("Voto no válido. Revisa el orden de los nombres").
*   **FR-40**: IF the vote submission fails with 401 (expired token), THEN THE SYSTEM MUST prompt for re-authentication and retry the submission with the new token.
*   **FR-84**: IF the list is still in VOTING phase but `ListResponse.myStepCompleted` is `true` (the Participant already voted in the current round and is waiting for the other members), THEN THE SYSTEM MUST render the voting screen in read-only mode: the ranking list and "Enviar voto" MUST be disabled and a waiting message ("Esperando a que el resto vote") MUST be displayed.

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

### 11. Invitation Sharing

*   **FR-67**: WHEN the dashboard is rendered and a list has `invitationsOpen` set to `true`, THEN THE SYSTEM MUST render an "Invitar" action on that list's card (`ui-list-card`).
*   **FR-68**: WHEN the Participant clicks "Invitar" (from the dashboard card or from the suggestion screen header — FR-82), THE SYSTEM MUST display the shared invitation modal (`ui-invite-modal`) showing the list name and its `invitationCode` using the `ui-copy-field` molecule.
*   **FR-69**: WHEN the Participant clicks "Copiar" in the invitation modal, THE SYSTEM MUST copy the code to the clipboard and display the confirmation message ("Código copiado").
*   **FR-70**: IF a list has `invitationsOpen` set to `false`, THEN THE SYSTEM MUST NOT render the "Invitar" action on its card.
*   **FR-71**: IF the clipboard is unavailable, THEN THE SYSTEM MUST display the code in a read-only, pre-selected input so the Participant can copy manually, alongside the message ("Copia el código manualmente").

### 12. Administration

*   **FR-72**: WHEN the Participant authenticates successfully, THE SYSTEM MUST fetch the user's role via `GET /api/v1/auth/me` and store the profile in the session state. THE SYSTEM MUST NOT decode JWTs client-side to obtain the role (NFR-1).
*   **FR-73**: WHEN the stored role is `ADMIN`, THE SYSTEM MUST render an admin action in the `ui-app-bar` that navigates to `/admin`. IF the role is not `ADMIN`, THEN THE SYSTEM MUST NOT render it.
*   **FR-74**: WHEN the Participant navigates to `/admin`, THE SYSTEM MUST call `GET /api/v1/admin/users` and display the returned users (id, username, role) in a list.
*   **FR-75**: WHEN the Administrator selects a user and requests a password reset, THE SYSTEM MUST display a modal with a new-password field validated with the same client-side complexity rules as registration (FR-56).
*   **FR-76**: WHEN the Administrator submits a valid new password, THE SYSTEM MUST call `PATCH /api/v1/admin/users/{id}/password`. IF it returns 200, THEN THE SYSTEM MUST close the modal and display the success message ("Contraseña actualizada").
*   **FR-77**: IF the password reset fails (400/404), THEN THE SYSTEM MUST display the backend error message inline and keep the modal open.
*   **FR-78**: WHEN a non-ADMIN navigates to `/admin`, THEN THE SYSTEM MUST redirect to the home dashboard. WHEN an unauthenticated user navigates to `/admin`, THEN THE SYSTEM MUST redirect to the login screen.

### 13. Dark Theme & Responsive Layout

*   **FR-88**: THE SYSTEM MUST render the application in the dark theme by default, driven entirely by the `ui-kit` design tokens (Spec 006 FR-51) with `color-scheme: dark`. No page-level hard-coded colors are allowed.
*   **FR-89**: IF the viewport is narrower than `640px`, THEN THE SYSTEM MUST keep every screen usable without horizontal scrolling: the app bar collapses and hides the user label, the dashboard actions wrap, and every page container uses `min(100% - 2 * gutter, ...)` gutters.
*   **FR-90**: WHEN the viewport is at least `768px` wide, THEN THE SYSTEM MUST render the dashboard list cards in a responsive grid (`repeat(auto-fill, minmax(280px, 1fr))`) inside a container up to `1024px` wide, and the flow pages (suggestion, selection, vote, results, login, register) MAY widen up to `720px`.
*   **FR-91**: WHEN the viewport is narrower than `640px`, THEN THE SYSTEM MUST make the admin user list horizontally scrollable instead of overflowing the screen.
*   **FR-92**: EVERY interactive control MUST keep a touch target of at least `44px` height where feasible on small viewports.

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
*   **Invitations Closed**: Once `invitationsOpen` is `false`, the "Invitar" action disappears from the card (FR-70).
*   **Clipboard Unavailable**: The invitation modal falls back to a read-only pre-selected input with a manual-copy message (FR-71).
*   **Profile Fetch Failure on Login**: IF fetching `/auth/me` fails after authentication, THEN THE SYSTEM MUST continue operating without role-gated UI (the admin action stays hidden) and retry the profile fetch on a later visit.
*   **Reset Target Not Found**: IF `PATCH /api/v1/admin/users/{id}/password` returns 404, THEN THE SYSTEM MUST display the backend error and keep the reset modal open (FR-77).
*   **Finished Phase Waiting for Others**: Once a Participant finishes their current phase step (`myStepCompleted=true`) but the list phase has not advanced, the corresponding screen MUST switch to read-only mode with a waiting message (FR-83, FR-86, FR-84). The Participant cannot modify names or re-vote until the other members finish.
*   **Voting Pool Includes Common Names**: The voting `currentPool` returned by `GET /api/v1/lists/{id}` MUST contain common names plus adopted faded names, so common names are never missing from a round (Spec 004 FR-6).
*   **Narrow Viewport (≤360px)**: Every authenticated screen MUST fit without horizontal scrolling: containers use `min(100% - 2 * gutter, ...)`, the app bar wraps and hides the user label, and the dashboard cards stack in a single column (FR-89).
*   **Wide Viewport (≥1024px)**: The dashboard uses a multi-column card grid and the admin screen widens; no fixed `480px` ceiling applies (FR-90, FR-91).

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
*   **TS-41**: Owner with `invitationsOpen=true` → card shows "Invitar"; clicking opens a modal with the list name and code.
*   **TS-42**: Click "Copiar" → clipboard contains the code and "Código copiado" is shown.
*   **TS-43**: Card with `invitationsOpen=false` renders no "Invitar" action.
*   **TS-44**: Clipboard unavailable → read-only pre-selected input and manual-copy message shown.
*   **TS-45**: Authenticated user fetches the profile via `/auth/me`; an `ADMIN` sees the admin action, a `PARTICIPANT` does not.
*   **TS-46**: Admin navigates to `/admin` → users list displayed.
*   **TS-47**: Admin resets a user's password successfully → 200, modal closed, "Contraseña actualizada" shown.
*   **TS-48**: Reset with a weak password → client-side field errors shown, no request sent.
*   **TS-49**: Reset fails (e.g., 404) → backend error shown inline, modal stays open.
*   **TS-50**: Non-admin navigating to `/admin` is redirected to the dashboard.
*   **TS-51**: Unauthenticated user navigating to `/admin` is redirected to login.
*   **TS-52**: The register flow sends exactly the typed password (no trimming or transformation) and a subsequent login with the same credentials succeeds (regression guard).
*   **TS-53**: Register fails with a 400 and a known password-rejection message → the specific message is shown inline on the password field, form retained.
*   **TS-54**: Register fails with a 400 empty-fields message → "El usuario es obligatorio" and "La contraseña es obligatoria" shown on their respective fields.
*   **TS-55**: Register fails with an unrecognized 400 → generic "Verifica los datos introducidos" shown, form retained.
*   **TS-56**: Selection view marks adopted suggestions and common names as selected (check indicator) and grays out unadopted faded suggestions.
*   **TS-57**: "Mis nombres" is rendered in a collapsible section with a smaller summary label that preserves the name count.
*   **TS-58**: Admin/selection waiting lock: with `myStepCompleted=true` and the list still in SELECTION, adoption buttons and "Completar selección" are disabled and the waiting message is shown.
*   **TS-59**: Suggestion waiting lock: with `myStepCompleted=true` and the list still in ADDITION, the input, delete actions and "Terminar Fase" are disabled, submitted names are fetched and displayed, and the waiting message is shown.
*   **TS-60**: Voting waiting lock: with `myStepCompleted=true` and the list still in VOTING, the ranking list and "Enviar voto" are disabled and the waiting message is shown.
*   **TS-61**: The suggestion screen renders the "Invitar" action when `invitationsOpen=true` and opens `ui-invite-modal` with the list code; it does not render it when `invitationsOpen=false`.
*   **TS-62**: Invitation modal (`ui-invite-modal`) shows the copied confirmation on successful copy and the manual-copy message on clipboard failure.
*   **TS-63**: The application renders the dark theme by default: `_variables.scss` defines dark token values with `color-scheme: dark`, and the `ui-button` danger hover uses the `--ui-color-danger-hover` token (no hard-coded color).
*   **TS-64**: The dashboard renders its list cards inside a `dashboard__grid` container using `repeat(auto-fill, minmax(280px, 1fr))` and a page container up to `1024px` wide.
*   **TS-65**: Every flow page container (suggestion, selection, vote, results, login, register) uses `min(100% - 2 * gutter, ...)` so a `360px` viewport renders without horizontal scrolling.
*   **TS-66**: The admin screen wraps its user list in a horizontally scrollable container for viewports narrower than `640px`.

---

## Out of Scope
*   Real-time collaborative updates (WebSockets) during suggestion or voting phases.
*   Automatic matching or grouping of similar names (backend concern, Spec 003).
*   Elimination round UI logic (round transitions are automated by the backend, Spec 004). The frontend only reacts to the `currentRound` and `totalRounds` values returned by the API.
*   Token refresh logic (Spec 001).
*   Runtime theme switching (user light/dark toggle).
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
*   Invitation sharing: "Invitar" action on open lists, invitation modal with copy-to-clipboard and clipboard fallback (FR-67..FR-71).
*   Administration: profile fetch via `/auth/me`, ADMIN-only app-bar action, `/admin` screen listing users, and the password-reset modal (FR-72..FR-78).
*   Dark theme: the app renders the dark palette by default with no hard-coded page colors (FR-88).
*   Responsive layout: app bar collapses on small viewports, the dashboard card grid adapts at ≥768px, flow pages widen at ≥768px, and the admin list scrolls horizontally under 640px (FR-89..FR-92).
*   Cache management verified: clear on success, retain on error, 5-minute dashboard refresh.
*   Full test suite passing (`npm test`) with zero lint warnings (`npm run lint`).