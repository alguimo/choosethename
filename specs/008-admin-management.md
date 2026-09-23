# Spec 008 — Administrator User Management

## Context & Objectives
Administrators currently have no way to manage user accounts through the API: the only provisioning path is out-of-band against the infrastructure. This specification adds ADMIN-only operations to list registered users and reset their passwords (the lost-password / account-recovery case for self-registered users), and exposes the authenticated user's role to the frontend via `GET /api/v1/auth/me` so role-gated UI (Spec 005) can be built. The password reset is a direct admin-set flow (MVP); hardened flows with single-use tokens are explicitly out of scope for this iteration.

## Users / Actors
* **Administrator**: A user with the `ADMIN` role who manages participant accounts.
* **Participant**: A regular user whose role must be exposed to the frontend through a contract endpoint.

## User Stories
* **US-1**: As an Administrator, I want to list all registered users, so that I can see who has an account and with which role.
* **US-2**: As an Administrator, I want to reset a user's password, so that a participant who lost their credentials can regain access.
* **US-3**: As a Participant, I want my role exposed through an authenticated endpoint, so that the frontend can render role-dependent UI without decoding JWTs.

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Role Exposure
* **FR-1**: WHEN an authenticated user requests `GET /api/v1/auth/me`, THE SYSTEM MUST return the authenticated user's profile with `id`, `username`, and `role`.
* **FR-2**: IF an unauthenticated request is made to `GET /api/v1/auth/me`, THEN THE SYSTEM MUST return 401 Unauthorized.

### 2. User Listing
* **FR-3**: WHEN an Administrator requests `GET /api/v1/admin/users`, THE SYSTEM MUST return the list of registered users (`id`, `username`, `role`) ordered by `id` ascending.
* **FR-4**: IF a non-Administrator requests `GET /api/v1/admin/users`, THEN THE SYSTEM MUST return 403 Forbidden.
* **FR-5**: IF an unauthenticated request is made to `GET /api/v1/admin/users`, THEN THE SYSTEM MUST return 401 Unauthorized.

### 3. Password Reset
* **FR-6**: WHEN an Administrator requests `PATCH /api/v1/admin/users/{id}/password` with a valid new password, THE SYSTEM MUST hash it using BCrypt (cost factor 12, as per Spec 001) and persist it, returning 200.
* **FR-7**: IF the new password does not meet the Spec 001 complexity rules (minimum 8 characters, at least one uppercase letter, at least one digit), THEN THE SYSTEM MUST return 400 Bad Request.
* **FR-8**: IF the target user does not exist, THEN THE SYSTEM MUST return 404 Not Found.
* **FR-9**: IF the caller is not an Administrator, THEN THE SYSTEM MUST return 403 Forbidden.
* **FR-10**: THE SYSTEM MUST NOT expose `passwordHash` in any response payload.

## Non-Functional Requirements
* **NFR-1 (Security)**: Administrator operations MUST be restricted to the `ADMIN` role using method-level security (`@EnableMethodSecurity` with `@PreAuthorize("hasRole('ADMIN')")`); the role is already present in the JWT `roles` claim.
* **NFR-2 (Decoupling)**: Access control lives entirely in the backend. The frontend only reacts to the 401/403 status codes it receives; no role logic is enforced in the UI.
* **NFR-3 (Language)**: All code, endpoints, fields, and messages in English; the UI translates user-facing text to Spanish.
* **NFR-4 (Dependencies)**: This specification MUST NOT add external dependencies to `pom.xml` or `package.json`.

## Edge Cases
* **Administrator resetting their own password**: permitted; the same Spec 001 complexity rules apply.
* **Blank or whitespace-only password**: rejected with 400 (same validation as registration).
* **Login rate limiting**: Spec 001 login rate limiting applies to the new password being used in subsequent logins, not to the admin reset endpoint itself.
* **Self-registered accounts**: only `PARTICIPANT` accounts can be created via `/auth/register`; the `ADMIN` role is never assignable through the public API (Spec 001 / Spec 008 NFR-1).

## Out of Scope
* Single-use reset tokens, expiry, and audit trail (hardened flow — deferred, Spec 001 P3 backlog).
* Self-service "forgot password" for participants.
* Creating or deleting user accounts, or changing roles through the API.

## Initial Test Scenarios (TDD Requirement)
* **TS-1**: `GET /api/v1/admin/users` as an Administrator returns 200 with all registered users.
* **TS-2**: `GET /api/v1/admin/users` as a Participant returns 403.
* **TS-3**: `PATCH /api/v1/admin/users/{id}/password` as an Administrator with a valid password returns 200 and the new password works on a subsequent login.
* **TS-4**: `PATCH /api/v1/admin/users/{id}/password` with a weak password returns 400.
* **TS-5**: `PATCH /api/v1/admin/users/{id}/password` with a non-existent user id returns 404.
* **TS-6**: `GET /api/v1/auth/me` with a token returns `id`, `username`, and `role`.
* **TS-7**: `GET /api/v1/auth/me` and `GET /api/v1/admin/users` without a token return 401.
* **TS-8**: No admin response payload contains `passwordHash` (asserted by integration tests).

## Completion Criteria
* All admin endpoints implemented with `@PreAuthorize` and covered by passing integration tests (TS-1..TS-8).
* `GET /api/v1/auth/me` covered by Spec 001 TS-6/TS-7 and consumed by the frontend (Spec 005 Section 12).
* Frontend admin flows verified by `npm test`; the full backend and frontend suites pass 100% with zero lint warnings.