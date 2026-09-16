# Spec 004 — Backend Voting Rounds

## Context & Objectives
This specification defines the final collaborative phase: voting. It establishes the automated logic for scoring ranked names, managing elimination rounds based on the total name count (15+ or <=15), and finalizing the top-3 names. The system automates round transitions when all list members have submitted their votes.

## Users / Actors
* **Participant**: An authenticated user (list member) who submits ranked votes.

## User Stories
* **US-1**: As a Participant, I want to submit my ranked preferences for a list of names so that the system can calculate our collective top choices.
* **US-2**: As a Participant, I want the system to automatically move us to the next round once ALL members submit our votes.

## Functional Requirements (Acceptance Criteria in EARS format)

### 1. Voting Process
* **FR-1**: WHEN all list members submit their ranked preferences for the current voting round, THE SYSTEM MUST calculate consolidated scores and transition the list to the next state.
* **FR-2**: IF the current round is not the last round, THEN THE SYSTEM MUST increment the `current_round` counter and keep the list in "VOTING" phase.
* **FR-3**: IF the current round is the last round, THEN THE SYSTEM MUST set the list phase to "COMPLETED" and expose the top 3 names.

### 2. Elimination Logic
* **FR-4**: IF there are more than 15 names in the pool at the start of the VOTING phase, THEN:
  * Round 1 keeps top 10.
  * Round 2 keeps top 5.
  * Round 3 (Final) keeps top 3.
* **FR-5**: IF there are 15 or fewer names in the pool at the start of the VOTING phase, THEN:
  * Round 1 keeps top 5.
  * Round 2 (Final) keeps top 3.

## Non-Functional Requirements
* **NFR-1 (Consistency)**: Voting scores must be stored atomically using optimistic locking to prevent concurrent update conflicts.
* **NFR-2 (Tie-breaking)**: When scores are equal at elimination thresholds or for final ranking positions (1-3), the system must break ties using the alphabetical order of the normalized name.

## Edge Cases
* **Desynchronized Submissions**: If only some members submit, the system stores the individual votes but does not advance the round.
* **Round Finalization**: If fewer names remain after a filter step than the target threshold, the surviving pool for the next round contains all remaining names.
* **Timeout**: Lists in VOTING phase that remain inactive for >48 hours must transition to "EXPIRED".

## Out of Scope
* UI components for dragging and dropping names.

## Completion Criteria
* Voting logic service implemented with full unit test coverage.
* Automatic round transition triggers verified.
* Integration tests covering both >15 and <=15 name scenarios, including concurrency and timeout, passing 100%.
