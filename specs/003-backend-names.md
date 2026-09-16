# Spec 003 — Backend Names Addition and Selection

## Context & Objectives
This specification covers the name input and matching phase. Participants add names independently to their private pools. Once both finish, the system normalizes these names (trimming whitespace, collapsing multiple spaces, ignoring accents, and converting to lowercase), aggregates them, and allows users to transition to a selection/consolidation phase. This ensures name privacy during entry and transparency during selection.

List phase transitions are final and irreversible. To prevent a list from remaining stuck forever, the system enforces a timeout that expires lists staying too long in the addition phase.

## Users / Actors
* **Participant**: An authenticated user who is a member of an active list.

## User Stories
* **US-1**: As a Participant, I want to add names to my private pool so that the other participant cannot see my choices yet.
* **US-2**: As a Participant, I want to signal that I have finished adding my names.
* **US-3**: As a Participant, I want to see the consolidated list of common names (matched) and exclusive suggestions (faded) so that I can refine our shared list.

## Domain States
| State | Description | Terminal |
|---|---|---|
| ADDITION | Initial state. Participants add names privately. | No — transitions to SELECTION (both FINISHED) or EXPIRED (timeout). |
| SELECTION | Participants inspect common names and faded suggestions, and adopt faded names into the shared pool. | No — transitions to VOTING (both COMPLETED). |
| VOTING | Voting rounds. Out of scope for this spec. | Yes (within scope). |
| EXPIRED | Reached when a list stays in ADDITION for more than 48 hours. | Yes. |

All phase transitions are final and irreversible.

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Name Addition Phase
* **FR-1**: WHILE the list is in the "ADDITION" phase, THE SYSTEM MUST ensure that a Participant can only read or modify their own name entries.
* **FR-2**: WHEN a Participant adds a name, THE SYSTEM MUST normalize it in accordance with NFR-1 and save it to the database linked to that user and list.
* **FR-3**: IF a Participant attempts to add a duplicate normalized name to their own list, THEN THE SYSTEM MUST reject the entry with a 422 Unprocessable Entity returned to the frontend for handling (English message per NFR-3).
* **FR-9**: IF a Participant marks their addition phase as "FINISHED" with zero names in their private pool, THEN THE SYSTEM MUST reject the request with a 400 Bad Request and return the message "At least one name must be provided to proceed to the selection phase."

### 2. Selection and Matching Phase
* **FR-5**: WHILE the list is in the "SELECTION" phase, THE SYSTEM MUST return a list containing "Common Names" (normalized matches between both pools) and "Faded Suggestions" (names existing in only one pool).
* **FR-6**: WHEN a Participant requests to adopt a name from the faded suggestions, THE SYSTEM MUST add it to the shared list pool (normalized per NFR-1).

### 3. Phase Transitions and Timeout
* **FR-4**: WHEN both participants mark their addition phase as "FINISHED", THE SYSTEM MUST transition the list phase to "SELECTION". This transition is final and irreversible.
* **FR-7**: WHEN both participants mark the selection phase as "COMPLETED", THE SYSTEM MUST transition the list phase to "VOTING". This transition is final and irreversible.
* **FR-8**: IF a list remains in the "ADDITION" phase for more than 48 hours, THEN THE SYSTEM MUST transition the list to the terminal "EXPIRED" phase.

## Non-Functional Requirements
* **NFR-1 (Normalization)**: The backend is the single source of truth for normalization. For every name that is stored, compared, or returned, the backend MUST apply, in this order: (1) trim leading and trailing whitespace, (2) collapse consecutive whitespace into a single space, (3) strip accents using Java's `Normalizer` (NFD form, removing diacritical marks), and (4) convert to lowercase. The frontend MUST NOT persist its own normalized values.
* **NFR-2 (Privacy)**: API endpoints in the "ADDITION" phase MUST filter out names that belong to the other participant.
* **NFR-3 (API Language)**: All backend API responses and error messages MUST be written in English. Localization (e.g., Spanish UI text) MUST be handled exclusively by the frontend.
* **NFR-4 (Concurrency)**: Phase transitions (ADDITION → SELECTION, SELECTION → VOTING, ADDITION → EXPIRED) MUST be guarded by optimistic locking (e.g., a JPA `@Version` field on the list entity). IF two participants trigger the same transition concurrently, THEN exactly one MUST succeed and the other MUST receive a 409 Conflict.

## Edge Cases
* **Empty Addition**: Rejected with a 400 Bad Request and the message defined in FR-9.
* **Name empty after normalization**: IF a submitted name normalizes to an empty string (e.g., input consisting only of whitespace), THEN THE SYSTEM MUST reject it with a 422 Unprocessable Entity.
* **Whitespace**: Consecutive whitespace within a name is collapsed into a single space (NFR-1). A name consisting only of whitespace normalizes to an empty string and is rejected (see above).
* **Zero Matches**: IF normalized names do not overlap between pools, THEN the "Common Names" list is returned empty, and participants must rely on the "Faded Suggestions" list to form the shared pool.
* **Concurrent transition**: Only one transition succeeds; the losing request receives a 409 Conflict (NFR-4).

## Out of Scope
* Automatic grouping by typos (phonetic or Levenshtein distance).
* Voting logic.
* Frontend localization (handled by the frontend layer per NFR-3).

## Completion Criteria
* Database tables (`names`, `shared_name_pool`) created via Flyway/Liquibase migrations.
* Normalization logic implemented and verified via unit tests, including whitespace collapsing and empty-after-normalization rejection.
* Addition and Selection endpoints covered by 100% passing automated JUnit integration tests.
* 48-hour timeout (ADDITION → EXPIRED) and concurrent-transition behavior (409 Conflict) covered by passing automated tests.