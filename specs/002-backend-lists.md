# Spec 002 — Backend Lists and Sharing

## Context & Objectives
This specification extends the Backend Foundation to introduce collaborative list management. It enables a Participant to create a name-deciding list, receive a unique, shareable 6-character alphanumeric invitation code valid for 48 hours, and invite up to 4 other Participants (maximum of 5 members per list for this MVP). The list owner can manually close invitations, and the system automatically closes invitations when the list reaches 5 members or when all members complete the initial name addition phase.

## Users / Actors
* **Participant**: An authenticated user who can create, join, or view active name lists.
* **List Owner**: The Participant who created the list and holds administrative actions (such as manually closing invitations).

## User Stories
* **US-1**: As a Participant, I want to create a new shared name list with a custom name so that I can start a collaborative selection process.
* **US-2**: As a Participant, I want to receive a unique shareable 6-character invitation code valid for 48 hours so that I can invite up to 4 other participants.
* **US-3**: As a Participant, I want to use an invitation code to join an existing shared list.
* **US-4**: As a List Owner, I want to manually close invitations for my list so that no additional participants can join.
* **US-5**: As a Participant, I want to retrieve my current active list details so that I can see its phase, code, and participating members.

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

### 3. Get Active List
* **Endpoint**: `GET /api/v1/lists/active`
* **Response (200 OK)**: Returns the active list DTO for the authenticated user.
* **Response (404 Not Found)**: Returned if the authenticated user is not a member of any active list.

### 4. Close Invitations
* **Endpoint**: `PATCH /api/v1/lists/{id}/close-invitations`
* **Response (200 OK)**: Returns updated list DTO with `invitationsOpen: false`.
* **Response (403 Forbidden)**: Returned if requested by a non-owner member.

---

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. List Creation
* **FR-1**: WHEN an authenticated Participant requests to create a new list with a non-blank name, THE SYSTEM MUST generate a unique 6-character alphanumeric invitation code (case-insensitive) valid for 48 hours, set `phase` to "ADDITION", set `invitationsOpen` to true, record the creator as owner and first member, and return a 201 Created with the list DTO.
* **FR-2**: IF a Participant already belongs to an active list (phase "ADDITION", "SELECTION", or "VOTING"), THEN THE SYSTEM MUST reject the creation request with a 400 Bad Request.
* **FR-3**: IF the list name is missing, null, or blank, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.

### 2. Joining a List
* **FR-4**: WHEN an authenticated Participant submits a valid, active invitation code (6 characters, case-insensitive), THE SYSTEM MUST link the Participant as a member of the list and return 200 OK with the updated list DTO.
* **FR-5**: IF the invitation code format is invalid (not exactly 6 alphanumeric characters), THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-6**: IF a Participant attempts to join using a non-existent invitation code, THEN THE SYSTEM MUST reject the request with a 404 Not Found.
* **FR-7**: IF a Participant attempts to join using an invitation code that has expired (older than 48 hours) or has `invitationsOpen` set to false, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-8**: IF a Participant attempts to join a list they are already a member of, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-9**: IF a Participant already belongs to another active list, THEN THE SYSTEM MUST reject the join request with a 400 Bad Request.
* **FR-10**: IF a Participant attempts to join a list that already has 5 members, THEN THE SYSTEM MUST reject the request with a 400 Bad Request.
* **FR-11**: WHEN joining a list causes the member count to reach 5, THE SYSTEM MUST automatically update `invitationsOpen` to false.

### 3. Managing Invitations
* **FR-12**: WHEN the List Owner requests to close invitations for their active list, THE SYSTEM MUST set `invitationsOpen` to false and return 200 OK.
* **FR-13**: IF a non-owner member attempts to close invitations, THEN THE SYSTEM MUST reject the request with a 403 Forbidden.
* **FR-14**: WHEN all current members of a list complete the "ADDITION" phase, THE SYSTEM MUST automatically set `invitationsOpen` to false.

### 4. Active List Information Retrieval
* **FR-15**: WHEN an authenticated Participant requests their active list details, THE SYSTEM MUST return the list ID, name, owner username, current phase ("ADDITION", "SELECTION", "VOTING", or "COMPLETED"), invitation code, expiration timestamp, `invitationsOpen` status, and member usernames.
* **FR-16**: IF an authenticated Participant does not belong to any active list, THEN THE SYSTEM MUST return a 404 Not Found response.

---

## Initial Test Scenarios (TDD Requirement)
* **TS-1**: Create list successfully with valid name (expect 201 Created, code length 6, expiration +48h, owner in members).
* **TS-2**: Create list when user already belongs to an active list (expect 400 Bad Request).
* **TS-3**: Create list with empty or blank name (expect 400 Bad Request).
* **TS-4**: Join list with valid invitation code (expect 200 OK, member added).
* **TS-5**: Join list with malformed code string (expect 400 Bad Request).
* **TS-6**: Join list with non-existent code (expect 404 Not Found).
* **TS-7**: Join list with expired code or closed invitations (expect 400 Bad Request).
* **TS-8**: Join list when list already has 5 members (expect 400 Bad Request).
* **TS-9**: Join 5th member auto-closes invitations (`invitationsOpen` becomes false).
* **TS-10**: Join list when user is already a member (expect 400 Bad Request).
* **TS-11**: Close invitations as List Owner (expect 200 OK, `invitationsOpen` becomes false).
* **TS-12**: Close invitations as non-owner member (expect 403 Forbidden).
* **TS-13**: Fetch active list when user is a member (expect 200 OK with complete details).
* **TS-14**: Fetch active list when user has no active list (expect 404 Not Found).

---

## Non-Functional Requirements
* **NFR-1 (Security)**: All endpoints in this spec require a valid JWT token. Users can only fetch or modify lists they belong to.
* **NFR-2 (Contract & Mapping)**: MapStruct mappers must convert list entities and memberships to `ListResponseDTO` to hide internal database IDs/entities.
* **NFR-3 (Case Insensitivity)**: Invitation codes must be converted to uppercase for storage and comparison to ensure case-insensitive matching.

---

## Edge Cases
* **Race Condition on Joining**: If multiple users concurrently attempt to join a list near the 5-member limit, database constraints/locking must prevent membership from exceeding 5.
* **Validation Precedence**:
  1. Validate request DTO / code format (`400 Bad Request`).
  2. Validate user active list status (`400 Bad Request`).
  3. Search code in database (`404 Not Found` if not present).
  4. Validate expiration, open status, membership limit, and duplicate membership (`400 Bad Request`).

---

## Out of Scope
* Adding names to lists, matching, or voting rounds (covered in Spec 003 and Spec 004).
* Managing multiple concurrent active lists per user (each user is limited to 1 active list in phase `ADDITION`, `SELECTION`, or `VOTING`).

---

## Completion Criteria
* Database schema migration (`V2__create_lists_tables.sql`) applied via Flyway.
* JPA entities (`ListEntity`, `ListMembershipEntity`), DTOs, and MapStruct mappers fully implemented.
* Invitation code generator service tested and verified.
* Endpoints covered by 100% passing automated JUnit integration tests (`TS-1` to `TS-14`).
