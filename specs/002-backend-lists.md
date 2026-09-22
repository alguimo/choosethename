# Spec 002 — Backend Lists and Sharing

## Context & Objectives
This specification extends the Backend Foundation to introduce collaborative list management. It enables a Participant to create name-deciding lists, receive a unique, shareable 6-character alphanumeric invitation code valid for 48 hours, and invite up to 4 other Participants (maximum of 5 members per list for this MVP). The list owner can manually close invitations, and the system automatically closes invitations when the list reaches 5 members or when all members complete the initial name addition phase.

A user may belong to **multiple lists at the same time**; creating or joining a list is not restricted by membership in other lists. The dashboard retrieves every list the user belongs to, including completed lists, so that results remain consultable.

## Users / Actors
* **Participant**: An authenticated user who can create, join, or view their name lists.
* **List Owner**: The Participant who created a list and holds administrative actions (such as manually closing invitations).

## User Stories
* **US-1**: As a Participant, I want to create a new shared name list with a custom name so that I can start a collaborative selection process.
* **US-2**: As a Participant, I want to receive a unique shareable 6-character invitation code valid for 48 hours so that I can invite up to 4 other participants.
* **US-3**: As a Participant, I want to use an invitation code to join an existing shared list.
* **US-4**: As a List Owner, I want to manually close invitations for my list so that no additional participants can join.
* **US-5**: As a Participant, I want to retrieve all the lists I belong to so that I can see their phase, code, and participating members, including completed lists whose results I can revisit.

## API Endpoint Contract

### 1. Create List
* **Endpoint**: `POST /api/v1/lists`
* **Request Body**:
  ```json
  {
    "name": "Baby Names 2026"
  }
  ```
* **Response (201 Created)**:
  ```json
  {
    "id": 1,
    "name": "Baby Names 2026",
    "invitationCode": "A1B2C3",
    "codeExpiresAt": "2026-09-16T10:00:00Z",
    "phase": "ADDITION",
    "ownerUsername": "alvaro",
    "members": ["alvaro"],
    "invitationsOpen": true
  }
  ```

### 2. Join List
* **Endpoint**: `POST /api/v1/lists/join`
* **Request Body**:
  ```json
  {
    "code": "A1B2C3"
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "id": 1,
    "name": "Baby Names 2026",
    "invitationCode": "A1B2C3",
    "codeExpiresAt": "2026-09-16T10:00:00Z",
    "phase": "ADDITION",
    "ownerUsername": "alvaro",
    "members": ["alvaro", "maria"],
    "invitationsOpen": true
  }
  ```

### 3. List My Lists
* **Endpoint**: `GET /api/v1/lists`
* **Response (200 OK)**: Returns an array of `ListResponseDTO` for every list the authenticated user belongs to (as owner or member) in phases `ADDITION`, `SELECTION`, `VOTING`, or `COMPLETED`, ordered by creation date descending. Returns an empty array when the user belongs to no visible list.

### 4. Get List by Id
* **Endpoint**: `GET /api/v1/lists/{id}`
* **Response (200 OK)**: Returns the `ListResponseDTO` for the list when the authenticated user is the owner or a member. A list in `COMPLETED` phase remains retrievable by its members.
* **Response (404 Not Found)**: Returned when the user is not the owner and not a member of the list, or when the list does not exist (same response in both cases to avoid leaking list existence).

### 5. Close Invitations
* **Endpoint**: `PATCH /api/v1/lists/{id}/close-invitations`
* **Response (200 OK)**: Returns updated list DTO with `invitationsOpen: false`.
* **Response (403 Forbidden)**: Returned if requested by a non-owner member.

---

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. List Creation
* **FR-1**: WHEN an authenticated Participant requests to create a new list with a non-blank name, THE SYSTEM MUST generate a unique 6-character alphanumeric invitation code (case-insensitive) valid for 48 hours, set `phase` to "ADDITION", set `invitationsOpen` to true, record the creator as owner and first member, and return a 201 Created with the list DTO. The Participant MAY create new lists even while already a member of other lists.
* **FR-2**: IF the list name is missing, null, or blank, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.

### 2. Joining a List
* **FR-3**: WHEN an authenticated Participant submits a valid, active invitation code (6 characters, case-insensitive), THE SYSTEM MUST link the Participant as a member of the list and return 200 OK with the updated list DTO. The Participant MAY join new lists even while already a member of other lists.
* **FR-4**: IF the invitation code format is invalid (not exactly 6 alphanumeric characters), THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-5**: IF a Participant attempts to join using a non-existent invitation code, THEN THE SYSTEM MUST reject the request with a 404 Not Found.
* **FR-6**: IF a Participant attempts to join using an invitation code that has expired (older than 48 hours) or has `invitationsOpen` set to false, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-7**: IF a Participant attempts to join a list they are already a member of, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-8**: IF a Participant attempts to join a list that already has 5 members, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-9**: WHEN joining a list causes the member count to reach 5, THE SYSTEM MUST automatically update `invitationsOpen` to false.

### 3. Managing Invitations
* **FR-10**: WHEN the List Owner requests to close invitations for their list, THE SYSTEM MUST set `invitationsOpen` to false and return 200 OK.
* **FR-11**: IF a non-owner member attempts to close invitations, THEN THE SYSTEM MUST reject the request with a 403 Forbidden.
* **FR-12**: WHEN all current members of a list complete the "ADDITION" phase, THE SYSTEM MUST automatically set `invitationsOpen` to false.

### 4. List Retrieval
* **FR-13**: WHEN an authenticated Participant requests `GET /api/v1/lists`, THE SYSTEM MUST return a 200 OK with an array of `ListResponseDTO` for every list the Participant belongs to (as owner or member) in phases `ADDITION`, `SELECTION`, `VOTING`, or `COMPLETED`, ordered by creation date descending. IF the Participant belongs to no such list, THEN THE SYSTEM MUST return an empty array.
* **FR-14**: WHEN an authenticated Participant who is the owner or a member of a list requests `GET /api/v1/lists/{id}`, THE SYSTEM MUST return a 200 OK with the `ListResponseDTO` containing the list ID, name, owner username, current phase ("ADDITION", "SELECTION", "VOTING", or "COMPLETED"), invitation code, expiration timestamp, `invitationsOpen` status, member usernames, current round, total rounds, and current pool.
* **FR-15**: IF the requesting user is not the owner and not a member of the list, OR the list does not exist, THEN THE SYSTEM MUST reject the request with a 404 Not Found. Both cases MUST produce the same response so that list existence is not leaked.
* **FR-16**: A list in `COMPLETED` phase MUST remain retrievable via `GET /api/v1/lists/{id}` for its members so that final results can be consulted.
* **FR-17**: IF the requested list is in the terminal `EXPIRED` phase, THEN THE SYSTEM MUST reject `GET /api/v1/lists/{id}` with a 404 Not Found, even for the owner or a member. EXPIRED lists are excluded from `GET /api/v1/lists` as well.

---

## Initial Test Scenarios (TDD Requirement)
* **TS-1**: Create list successfully with valid name (expect 201 Created, code length 6, expiration +48h, owner in members).
* **TS-2**: Create list when the user already belongs to another active list (expect 201 Created).
* **TS-3**: Create list with empty or blank name (expect 400 Bad Request).
* **TS-4**: Join list with valid invitation code (expect 200 OK, member added).
* **TS-5**: Join list with malformed code string (expect 400 Bad Request).
* **TS-6**: Join list with non-existent code (expect 404 Not Found).
* **TS-7**: Join list with expired code or closed invitations (expect 400 Bad Request).
* **TS-8**: Join list when list already has 5 members (expect 400 Bad Request).
* **TS-9**: Join 5th member auto-closes invitations (`invitationsOpen` becomes false).
* **TS-10**: Join list when user is already a member of that list (expect 400 Bad Request).
* **TS-11**: Close invitations as List Owner (expect 200 OK, `invitationsOpen` becomes false).
* **TS-12**: Close invitations as non-owner member (expect 403 Forbidden).
* **TS-13**: Fetch my lists when the user belongs to none (expect 200 OK with an empty array).
* **TS-14**: Fetch my lists with active and completed lists (expect 200 OK with all lists, ordered by creation date descending).
* **TS-15**: Fetch a list by id as a member (expect 200 OK with complete details).
* **TS-16**: Fetch a list by id as a non-member (expect 404 Not Found).
* **TS-17**: Fetch a list by id that does not exist (expect 404 Not Found).
* **TS-18**: Fetch a COMPLETED list by id as a member (expect 200 OK, results consultable).
* **TS-19**: Fetch an EXPIRED list by id as a member (expect 404 Not Found).

---

## Non-Functional Requirements
* **NFR-1 (Security)**: All endpoints in this spec require a valid JWT token. Users can only fetch or modify lists they belong to: `GET /api/v1/lists` returns only the caller's lists, and `GET /api/v1/lists/{id}` verifies that the caller is the owner or a member before returning details.
* **NFR-2 (Contract & Mapping)**: MapStruct mappers must convert list entities and memberships to `ListResponseDTO` to hide internal database IDs/entities.
* **NFR-3 (Case Insensitivity)**: Invitation codes must be converted to uppercase for storage and comparison to ensure case-insensitive matching.

---

## Edge Cases
* **Race Condition on Joining**: If multiple users concurrently attempt to join a list near the 5-member limit, database constraints/locking must prevent membership from exceeding 5.
* **Validation Precedence**:
  1. Validate request DTO / code format (`400 Bad Request`).
  2. Search code in database (`404 Not Found` if not present).
  3. Validate expiration, open status, membership limit, and duplicate membership (`400 Bad Request`).
* **No Existence Leak on List Retrieval**: `GET /api/v1/lists/{id}` returns 404 both for non-members and for non-existent ids (FR-15).
* **Completed Lists Remain Consultable**: A `COMPLETED` list is not an "active" list, but it MUST remain visible in `GET /api/v1/lists` and retrievable via `GET /api/v1/lists/{id}` for its members (FR-16).
* **Expired Lists Are Not Consultable**: An `EXPIRED` list is terminal and MUST return 404 from `GET /api/v1/lists/{id}` and MUST be absent from `GET /api/v1/lists`, even for its owner and members (FR-17).

---

## Out of Scope
* Adding names to lists, matching, or voting rounds (covered in Spec 003 and Spec 004).
* Voluntarily leaving or removing membership from a list.

---

## Completion Criteria
* Database schema migration (`V2__create_lists_tables.sql`) applied via Flyway.
* JPA entities (`ListEntity`, `ListMembershipEntity`), DTOs, and MapStruct mappers fully implemented.
* Invitation code generator service tested and verified.
* Multi-list membership supported: creating or joining a second list while in another list succeeds.
* List retrieval endpoints (`GET /api/v1/lists` and `GET /api/v1/lists/{id}`) covered by 100% passing automated JUnit integration tests (`TS-1` to `TS-18`).