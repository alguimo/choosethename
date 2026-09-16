# Project Constitution

## Preamble
This Constitution defines the core principles, governance rules, and software engineering standards for the project. All contributors (human and AI agents) must strictly adhere to these rules without exception.

---

## Principle 1: Strict Spec-Driven Development (SDD)
1.1. **Specification Precedence**: The active specification (`/specs/*.md`) is the single source of truth. No behavior, feature, or code modification may be implemented unless explicitly defined in an approved, active spec.
1.2. **Handling Ambiguity**: If any specification detail is missing, ambiguous, or incomplete, work MUST STOP immediately. The contributor/agent must ask the user for clarification before proceeding. Assumptions or unprompted decisions are forbidden.

---

## Principle 2: Strict Decoupling of Frontend and Backend
2.1. **Autonomous Architectural Layers**: Backend and Frontend must be completely decoupled and independently functional.
2.2. **Contract-Based Interaction**: Communication between Frontend and Backend relies strictly on clear API contracts. Replacing or refactoring either layer must only require adapting the contract layer, with zero impact on core domain logic.

---

## Principle 3: Test-First Definition & Mandatory Verification
3.1. **Test Definition Before Implementation**: Test scenarios and criteria MUST be defined before deciding on implementation details. This ensures tests validate functional requirements rather than confirming specific implementation artifacts.
3.2. **Mandatory Verification**: Every single task must complete with passing automated tests covering the newly added or updated functionality. A task is not complete until its tests pass.

---

## Principle 4: Language Policy
4.1. **Application Language**: The user interface (UI) and user-facing content of the application will be in **Spanish**.
4.2. **Engineering Language**: All code, variable/function naming, commit messages, documentation, comments, and Markdown (`.md`) files MUST be written in **English**.

---

## Principle 5: Technology Guidance
5.1. Tech stack choices, framework conventions, and specific operational tooling are declared and governed in `AGENTS.md`.
