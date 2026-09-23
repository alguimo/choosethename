# Contracts Spec — REST API Contract

## Source of Truth
The OpenAPI contract is defined in `specs/openapi.yaml`. The frontend and the backend communicate exclusively through these JSON REST operations; neither layer may invent endpoints outside this contract (Spec 005 NFR-7). Domain DTOs mirror the contract directly with no frontend-side normalization.

## Conventions
* **Base path**: `/api/v1`
* **Authentication**: stateless JWT (`Authorization: Bearer <token>`), valid 24 h, payload claims `sub` (username) and `roles`.
* **Error shape**: JSON `Error` object; domain exceptions map to HTTP statuses via `GlobalExceptionHandler`.
* **Language**: all contract fields and messages in English; the UI translates user-facing text to Spanish.

## Operations by Specification

### Spec 001 — Foundation
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Any user self-registers a Participant account (409 if username exists, 400 on invalid/blank input). |
| `POST` | `/api/v1/auth/login` | Participant logs in; returns JWT (401 on invalid credentials, 429 when rate-limited). |
| `GET` | `/api/v1/auth/me` | Caller's profile (`id`, `username`, `role`); 401 without a token. |
| `GET` | `/api/v1/test/protected` | Guarded smoke endpoint (401 without token). |

### Spec 002 — Lists & Sharing
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/lists` | Create a list with a 48 h invitation code. |
| `GET` | `/api/v1/lists` | All the caller's lists (own + joined), incl. COMPLETED; empty array if none. |
| `GET` | `/api/v1/lists/{id}` | List details; 404 for non-members, non-existent ids, and EXPIRED lists. |
| `POST` | `/api/v1/lists/join` | Join a list by code (multi-list membership allowed). |
| `PATCH` | `/api/v1/lists/{id}/close-invitations` | Owner closes invitations for the list. |

### Spec 003 — Names & Selection
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/lists/{id}/names` | Add names (normalized; duplicates → 422). |
| `POST` | `/api/v1/lists/{id}/finish-addition` | Finish the ADDITION phase for the caller. |
| `GET` | `/api/v1/lists/{id}/selection` | Selection view: Common vs. Faded suggestions. |
| `POST` | `/api/v1/lists/{id}/selection/adopt` | Adopt a faded name into the shared pool. |
| `POST` | `/api/v1/lists/{id}/complete-selection` | Complete the SELECTION phase for the caller. |

### Spec 004 — Voting & Results
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/lists/{id}/vote` | Submit a full ranking for the current round (overwrite allowed; staleness → 409). |
| `GET` | `/api/v1/lists/{id}/results` | Final ranked results when the list is COMPLETED. |

### Spec 008 — Administration
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/admin/users` | All registered users (`id`, `username`, `role`); ADMIN only, 403 for non-admins. |
| `PATCH` | `/api/v1/admin/users/{id}/password` | Reset a user's password (`ResetPasswordRequest`); 400 weak password, 404 unknown user, ADMIN only. |

## Core Data Models
* **ListResponse**: `id`, `name`, `invitationCode`, `codeExpiresAt`, `phase`, `invitationsOpen`, `ownerUsername`, `currentRound`, `totalRounds`, `currentPool`, `members` — root state for dashboard and phase routing.
* **NameEntry**: `name`, `normalizedName`.
* **AddNameRequest**: `names[]`.
* **VoteRequest**: `roundNumber`, `rankings[]`.
* **ResultsResponse**: ordered `results[]` of `{ rank, name, score }` (top-3 or fewer).
* **User**: `id`, `username`, `role` (`ADMIN` | `PARTICIPANT`) — profile returned by `/auth/register` and `/auth/me`.
* **ResetPasswordRequest**: `password` (new password meeting the Spec 001 complexity rules).
* **Error**: `error` message surfaced inline by the UI.

## Authorization Rules
All `/api/v1/lists/**` operations require a valid JWT and (for read/modify operations on a specific list) membership in that list: `GET /api/v1/lists` returns only the caller's own lists (excluding EXPIRED), and `GET /api/v1/lists/{id}` verifies membership, returning 404 for non-members, non-existent ids, and EXPIRED lists. Phase-bound operations (voting, finishing, completing) are rejected with 409 when the list phase does not allow them. All `/api/v1/admin/**` operations additionally require the `ROLE_ADMIN` authority; authenticated callers without it receive 403.

## Out of Scope

* Removing a user from a list (voluntary leave or membership removal) is intentionally out of scope for the MVP; see Spec 005's Out of Scope section for the recorded decision.
* Token revocation / server-side logout and hardened password reset (single-use tokens, expiry, audit trail) are deferred to a future iteration; see Spec 008 and G4 of the task plan.