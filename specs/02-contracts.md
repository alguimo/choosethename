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
| `POST` | `/api/v1/auth/register` | Administrator registers a Participant (409 if username exists, 400 on invalid/blank input). |
| `POST` | `/api/v1/auth/login` | Participant logs in; returns JWT (401 on invalid credentials). |
| `GET` | `/api/v1/test/protected` | Guarded smoke endpoint (401 without token). |

### Spec 002 — Lists & Sharing
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/lists` | Create a list with a 48 h invitation code. |
| `POST` | `/api/v1/lists/join` | Join a list by code. |
| `GET` | `/api/v1/lists/active` | Fetch the caller's active list. |
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

## Core Data Models
* **ListResponse**: `id`, `name`, `invitationCode`, `codeExpiresAt`, `phase`, `invitationsOpen`, `ownerUsername`, `currentRound`, `totalRounds`, `currentPool`, `members` — root state for dashboard and phase routing.
* **NameEntry**: `name`, `normalizedName`.
* **AddNameRequest**: `names[]`.
* **VoteRequest**: `roundNumber`, `rankings[]`.
* **ResultsResponse**: ordered `results[]` of `{ rank, name, score }` (top-3 or fewer).
* **Error**: `error` message surfaced inline by the UI.

## Authorization Rules
All `/api/v1/lists/**` operations require a valid JWT and list membership (403 otherwise). Phase-bound operations (voting, finishing, completing) are rejected with 409 when the list phase does not allow them.