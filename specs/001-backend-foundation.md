# Spec 001 — Backend Foundation

## Context & Objectives
This specification defines the core infrastructure, database setup, and user authentication mechanism for the Collaborative Name Decider Backend. The goal is to establish a secure, decoupled, and testable foundation before implementing any list or voting features. It enables any user to self-register a Participant account and allows Participants to securely log in.

## Users / Actors
* **Administrator**: A system role with elevated privileges. The first Administrator account is provisioned directly against the system infrastructure; it is not used for participant registration.
* **Participant**: A regular user who can self-register and authenticate to later interact with lists and names.

## User Stories
* **US-1**: As a new user, I want to register my own Participant account with a username and password so I can log in.
* **US-2**: As a Participant, I want to log in using my credentials so I can obtain an access token and interact with the API.

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Account Creation
* **FR-1**: WHEN a user requests to register a new Participant account, THE SYSTEM MUST validate that the password meets the complexity requirements (min 8 characters, 1 uppercase, 1 digit), hash it using BCrypt with a cost factor of 12, and save the Participant entity to the database.
* **FR-2**: IF a user attempts to register a Participant with a username that already exists, THEN THE SYSTEM MUST reject the operation with a 409 Conflict.

### 2. User Authentication
* **FR-3**: WHEN a Participant requests to log in with valid credentials, THE SYSTEM MUST return a success response containing a stateless JWT (valid for 24 hours). The JWT payload must contain the `sub` (username) and `roles` (authority) claims.
* **FR-4**: IF a Participant attempts to log in with incorrect credentials, THEN THE SYSTEM MUST return a 401 Unauthorized.
* **FR-5**: WHEN an API request is made without a valid authentication token, THE SYSTEM MUST reject the request with a 401 Unauthorized.

### 3. Current User Profile
* **FR-6**: WHEN an authenticated user requests `GET /api/v1/auth/me`, THE SYSTEM MUST return the authenticated user's profile with `id`, `username`, and `role`.

### 4. Login Rate Limiting
* **FR-7**: IF at least 5 failed login attempts occur for the same client IP within a rolling 15-minute window, THEN THE SYSTEM MUST reject subsequent login attempts from that IP with 429 Too Many Requests, AND THE SYSTEM MUST reset the failed-attempt counter for the IP whenever a login succeeds.

### 5. Cross-Origin Access
* **FR-8**: WHEN a request (including CORS preflight) arrives from an origin listed in the `CORS_ALLOWED_ORIGINS` environment variable, THEN THE SYSTEM MUST respond with the corresponding CORS headers. IF the origin is not allowed, THEN THE SYSTEM MUST NOT include CORS headers in the response.

## Initial Test Scenarios (TDD Requirement)
* **TS-1**: Register Participant successfully.
* **TS-2**: Register Participant with existing username (expect 409).
* **TS-3**: Login with valid credentials (expect JWT).
* **TS-4**: Login with invalid credentials (expect 401).
* **TS-5**: Access protected endpoint without token (expect 401).
* **TS-6**: `GET /api/v1/auth/me` with a valid token returns `id`, `username`, and `role`.
* **TS-7**: `GET /api/v1/auth/me` without a token returns 401.
* **TS-8**: Rate limiting: 5 failed logins from one IP return 401 and the 6th attempt within the window returns 429; a successful login resets the counter.
* **TS-9**: CORS: a preflight from an allowed origin receives CORS headers; a preflight from a disallowed origin does not.

## Non-Functional Requirements
* **NFR-1 (Tech Stack)**: Built on Java 17+ and Spring Boot 3.x using H2/PostgreSQL.
* **NFR-2 (Language)**: All backend code, variables, endpoints, and database fields must be written strictly in English.
* **NFR-3 (Decoupling)**: Client-Server communication must be completely decoupled via stateless JSON REST APIs. JWTs are handled entirely by the client (e.g., LocalStorage).
* **NFR-4 (Mapping)**: MapStruct must be used to map between entities (Database models) and DTOs (Data Transfer Objects).
* **NFR-5 (Migrations)**: Database schemas must be initialized and evolved using Flyway/Liquibase migration files, including a `UNIQUE CONSTRAINT` on the username column.
* **NFR-6 (Secrets)**: The JWT signing secret MUST be provided through the `JWT_SECRET` environment variable; the repository MUST NOT commit a production secret. The versioned `application.yml` MUST reference the environment variable. Test configuration MAY define a non-secret, test-only value.
* **NFR-7 (CORS)**: Cross-origin requests MUST be allowed only for origins configured through the `CORS_ALLOWED_ORIGINS` environment variable (an explicit `CorsConfigurationSource` bean). IF no origins are configured, THEN no cross-origin requests are allowed.

## Technical Implementation Notes
* **Test Isolation**: All integration tests must run against an ephemeral H2 database using `@DataJpaTest` or `@SpringBootTest` with a test-specific configuration to ensure atomicity.
* **BCrypt Config**: Use a static cost factor of 12 for password hashing.
* **JWT Secret Externalization**: `jwt.secret` MUST be bound via `${JWT_SECRET}` (no fallback literal) so the application fails fast when the variable is absent. The test `application.yml` keeps a hard-coded, test-only value per NFR-6.
* **Rate Limiter**: Implemented in-memory (sliding window keyed by client IP, e.g. `ConcurrentHashMap` with timestamps) with no external dependency. The limiter covers `POST /auth/login` only; other endpoints are not throttled in this iteration.

## Edge Cases
* **Empty/Blank Inputs**: If a registration or login request is sent with null, empty, or whitespace-only usernames/passwords, the system must reject it with a 400 Bad Request.
* **SQL Injection & Special Characters**: Usernames must be validated using an alphanumeric regex pattern to prevent injection attacks.
* **Rate-Limit Window Sliding**: A 429 response must not lock an IP forever; the window slides so older failed attempts expire. Successful logins reset the counter (FR-7).
* **Failed Login Without an Existing User**: Unknown usernames count as failed attempts for rate-limiting purposes (same 401/429 behavior).

## Out of Scope
* Passkey (WebAuthn) integration.
* Token refresh logic.
* List management, matching, and voting features.

## Completion Criteria
* Spring Boot project scaffolded and compiling.
* Database tables (`users`) initialized via Flyway/Liquibase migrations, including `UNIQUE` index on `username`.
* User entity, DTOs, and MapStruct mappers implemented.
* Account registration and login endpoints covered by 100% passing JUnit integration tests in an isolated environment.
