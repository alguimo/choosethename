# Spec 001 — Backend Foundation

## Context & Objectives
This specification defines the core infrastructure, database setup, and user authentication mechanism for the Collaborative Name Decider Backend. The goal is to establish a secure, decoupled, and testable foundation before implementing any list or voting features. It enables an Administrator to register new Participant accounts and allows Participants to securely log in.

## Users / Actors
* **Administrator**: A system role responsible for creating new Participant accounts. The first Administrator account is provisioned directly against the system infrastructure.
* **Participant**: A regular user who can authenticate and will later interact with lists and names.

## User Stories
* **US-1**: As an Administrator, I want to create Participant accounts with a username and password so they can log in.
* **US-2**: As a Participant, I want to log in using my credentials so I can obtain an access token and interact with the API.

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Account Creation
* **FR-1**: WHEN an Administrator requests to create a new Participant account, THE SYSTEM MUST validate that the password meets the complexity requirements (min 8 characters, 1 uppercase, 1 digit), hash it using BCrypt with a cost factor of 12, and save the Participant entity to the database.
* **FR-2**: IF an Administrator attempts to register a Participant with a username that already exists, THEN THE SYSTEM MUST reject the operation with a 409 Conflict.

### 2. User Authentication
* **FR-3**: WHEN a Participant requests to log in with valid credentials, THE SYSTEM MUST return a success response containing a stateless JWT (valid for 24 hours). The JWT payload must contain the `sub` (username) and `roles` (authority) claims.
* **FR-4**: IF a Participant attempts to log in with incorrect credentials, THEN THE SYSTEM MUST return a 401 Unauthorized.
* **FR-5**: WHEN an API request is made without a valid authentication token, THE SYSTEM MUST reject the request with a 401 Unauthorized.

## Initial Test Scenarios (TDD Requirement)
* **TS-1**: Register Participant successfully.
* **TS-2**: Register Participant with existing username (expect 409).
* **TS-3**: Login with valid credentials (expect JWT).
* **TS-4**: Login with invalid credentials (expect 401).
* **TS-5**: Access protected endpoint without token (expect 401).

## Non-Functional Requirements
* **NFR-1 (Tech Stack)**: Built on Java 17+ and Spring Boot 3.x using H2/PostgreSQL.
* **NFR-2 (Language)**: All backend code, variables, endpoints, and database fields must be written strictly in English.
* **NFR-3 (Decoupling)**: Client-Server communication must be completely decoupled via stateless JSON REST APIs. JWTs are handled entirely by the client (e.g., LocalStorage).
* **NFR-4 (Mapping)**: MapStruct must be used to map between entities (Database models) and DTOs (Data Transfer Objects).
* **NFR-5 (Migrations)**: Database schemas must be initialized and evolved using Flyway/Liquibase migration files, including a `UNIQUE CONSTRAINT` on the username column.

## Technical Implementation Notes
* **Test Isolation**: All integration tests must run against an ephemeral H2 database using `@DataJpaTest` or `@SpringBootTest` with a test-specific configuration to ensure atomicity.
* **BCrypt Config**: Use a static cost factor of 12 for password hashing.

## Edge Cases
* **Empty/Blank Inputs**: If a registration or login request is sent with null, empty, or whitespace-only usernames/passwords, the system must reject it with a 400 Bad Request.
* **SQL Injection & Special Characters**: Usernames must be validated using an alphanumeric regex pattern to prevent injection attacks.

## Out of Scope
* Self-service sign-up for Participants.
* Passkey (WebAuthn) integration.
* Token refresh logic.
* List management, matching, and voting features.

## Completion Criteria
* Spring Boot project scaffolded and compiling.
* Database tables (`users`) initialized via Flyway/Liquibase migrations, including `UNIQUE` index on `username`.
* User entity, DTOs, and MapStruct mappers implemented.
* Account registration and login endpoints covered by 100% passing JUnit integration tests in an isolated environment.
