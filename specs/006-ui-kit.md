# Spec 006 — UI Kit Library

## Context & Objectives
This specification defines the architecture and component standards for the `ui-kit` library, a reusable Angular 17 component library built within the current project. The library is designed to be independent from business logic and API contracts, enabling future extraction into a standalone package or reuse in other Angular projects.

The library uses **Angular Material + CDK** as the underlying UI primitive, ensuring accessibility (WCAG 2.1 AA), cross-browser consistency, and performant behavioral patterns (drag-and-drop, overlay management, focus trapping) without reinventing solved problems.

The library follows the **Atomic Design** methodology: components are organized into atoms (base elements), molecules (compositions of atoms), and organisms (complex compositions that approximate a UI section).

## Users / Actors
* **Library Consumer**: A developer (current or future) who imports `ui-kit` components into an Angular application.
* **End User**: A person interacting with the rendered UI components in the browser.

## User Stories
* **US-1**: As a Library Consumer, I want to import standalone Angular components from `ui-kit` so I can build UIs without re-implementing base elements.
* **US-2**: As a Library Consumer, I want to override the visual theme of `ui-kit` components using CSS custom properties so I can adapt them to different projects.
* **US-3**: As a Library Consumer, I want every component to follow a consistent API pattern (inputs, outputs, naming) so I can predict how to use any component without reading documentation.
* **US-4**: As a Library Consumer, I want to use `ui-kit` components with Angular CDK drag-and-drop so I can build sortable lists and ranking interfaces.
* **US-5**: As an End User, I want all interactive components to be fully keyboard-navigable and screen-reader accessible.

---

## Architecture

### 1. Directory Structure
The library lives at `src/app/ui-kit/` within the main project. Each tier has its own directory:
```
src/app/ui-kit/
  atoms/
    button/
      button.component.ts
      button.component.spec.ts
    input-field/
      input-field.component.ts
      input-field.component.spec.ts
    ...
  molecules/
    name-input-row/
      name-input-row.component.ts
      name-input-row.component.spec.ts
    ...
  organisms/
    draggable-ranking-list/
      draggable-ranking-list.component.ts
      draggable-ranking-list.component.spec.ts
    ...
  tokens/
    _variables.scss
  ui-kit.module.ts          (barrel export)
```

### 2. Design Tokens
All visual properties are defined as CSS custom properties in `tokens/_variables.scss`. Components reference these tokens instead of hard-coded values. The default palette is **dark** (`color-scheme: dark`); consumers can override any token at a lower cascade scope (e.g. a `.theme-light` class) without touching component code.

| Token | Default (dark) | Purpose |
|---|---|---|
| `--ui-color-primary` | `#6FA5F0` | Primary action color |
| `--ui-color-on-primary` | `#06121F` | Text/icon on primary |
| `--ui-color-primary-hover` | `#8AB6F5` | Primary hover state |
| `--ui-color-primary-container` | `#21324B` | Primary-tinted container |
| `--ui-color-on-primary-container` | `#C9DCFF` | Text on primary container |
| `--ui-color-danger` | `#E57373` | Error / destructive state |
| `--ui-color-on-danger` | `#2B0A0A` | Text on danger |
| `--ui-color-danger-hover` | `#EF9A9A` | Destructive hover state |
| `--ui-color-danger-container` | `#3A1E1E` | Destructive container |
| `--ui-color-on-danger-container` | `#FFDAD6` | Text on danger container |
| `--ui-color-success` | `#81C784` | Success state |
| `--ui-color-success-container` | `#1E3A24` | Success container |
| `--ui-color-on-success-container` | `#C8E6C9` | Text on success container |
| `--ui-color-warning` | `#FFB74D` | Warning state |
| `--ui-color-warning-container` | `#3A2E1E` | Warning container |
| `--ui-color-on-warning-container` | `#FFE0B2` | Text on warning container |
| `--ui-color-info` | `#4FC3F7` | Info state |
| `--ui-color-info-container` | `#1E3343` | Info container |
| `--ui-color-on-info` | `#06121F` | Text on info |
| `--ui-color-background` | `#14151A` | App background |
| `--ui-color-surface` | `#1D1E26` | Card / container background |
| `--ui-color-surface-variant` | `#262833` | Elevated / muted surface |
| `--ui-color-on-surface` | `#E8E6ED` | Text on surface |
| `--ui-color-on-surface-variant` | `#B9B7C3` | Secondary text |
| `--ui-color-outline` | `#8A8794` | Borders, dividers |
| `--ui-color-outline-variant` | `#3A3C47` | Subtle borders |
| `--ui-color-backdrop` | `rgba(0, 0, 0, 0.6)` | Modal backdrop |
| `--ui-color-focus-ring` | `rgba(62, 153, 249, 0.5)` | Focus outline |
| `--ui-layout-gutter` | `1rem` | Horizontal gutter for page containers |
| `--ui-layout-max-width` | `480px` | Page container max width (720px at ≥768px viewports) |
| `--ui-layout-page-width` | `min(100% - var(--ui-layout-gutter) * 2, var(--ui-layout-max-width))` | Derived page container width (keeps gutters on small viewports) |
| `--ui-breakpoint-sm` | `640px` | Small viewport threshold (see FR-52) |
| `--ui-radius-sm` | `4px` | Small radius (buttons, inputs) |
| `--ui-radius-md` | `8px` | Medium radius (cards, modals) |
| `--ui-spacing-xs` | `4px` | Tight spacing |
| `--ui-spacing-sm` | `8px` | Small spacing |
| `--ui-spacing-md` | `16px` | Default spacing |
| `--ui-spacing-lg` | `24px` | Section spacing |
| `--ui-font-family` | `'Inter', sans-serif` | Base typeface |
| `--ui-font-size-sm` | `0.875rem` | Labels, helper text |
| `--ui-font-size-base` | `1rem` | Body text |
| `--ui-font-size-lg` | `1.25rem` | Emphasis text |

---

## Functional Requirements (EARS)

### 1. Component Standards

*   **FR-1**: EVERY `ui-kit` component MUST be an Angular standalone component.
*   **FR-2**: EVERY `ui-kit` component MUST use `ChangeDetectionStrategy.OnPush` by default.
*   **FR-3**: EVERY `ui-kit` component MUST reference CSS custom properties from the token system instead of hard-coded values.
*   **FR-4**: EVERY interactive component (button, input, icon-button) MUST support full keyboard navigation (Tab, Enter, Escape, Arrow keys where applicable).
*   **FR-5**: EVERY `ui-kit` component MUST NOT import any service, model, or interface from the main application. The library receives data via `@Input()` and emits events via `@Output()`.
*   **FR-6**: EVERY `ui-kit` component selector MUST use the `ui-` prefix (e.g., `ui-button`, `ui-input-field`, `ui-list-card`).
*   **FR-7**: EVERY `ui-kit` component file MUST follow kebab-case naming for selectors and filenames (e.g., `name-input-row.component.ts`).
*   **FR-8**: EVERY `ui-kit` component MUST have a corresponding `.spec.ts` test file with a minimum of 2 test cases (rendering and primary interaction).

### 2. Atom Components

#### 2.1 ButtonComponent (`ui-button`)
*   **FR-9**: THE SYSTEM MUST provide a `ui-button` component with inputs: `label` (string), `variant` (enum: `primary`, `secondary`, `danger`, `ghost`), `disabled` (boolean), `loading` (boolean).
*   **FR-10**: WHEN `loading` is true, THE SYSTEM MUST display a Material spinner inside the button and disable interaction.
*   **FR-11**: THE SYSTEM MUST emit a `clicked` event when the button is activated via click or Enter key.

#### 2.2 InputFieldComponent (`ui-input-field`)
*   **FR-12**: THE SYSTEM MUST provide a `ui-input-field` component with inputs: `label` (string), `placeholder` (string), `value` (string), `error` (string), `disabled` (boolean), `maxLength` (number).
*   **FR-13**: THE SYSTEM MUST display a red border and an error message below the input when the `error` input is provided.
*   **FR-14**: THE SYSTEM MUST emit a `valueChanged` event on every input change and a `submitted` event on Enter key press.
*   **FR-42**: WHEN the `type` input is `password`, THE SYSTEM MUST render a visibility toggle button that switches the control's `type` between `password` and `text`, announcing the state with `aria-pressed` and switching the `aria-label` between "Mostrar contraseña" and "Ocultar contraseña".

#### 2.3 IconButtonComponent (`ui-icon-button`)
*   **FR-15**: THE SYSTEM MUST provide a `ui-icon-button` component with inputs: `icon` (string — Material icon name), `tooltip` (string), `disabled` (boolean), `variant` (enum: `default`, `danger`).
*   **FR-16**: THE SYSTEM MUST emit a `clicked` event when the button is activated.

#### 2.4 BadgeComponent (`ui-badge`)
*   **FR-17**: THE SYSTEM MUST provide a `ui-badge` component with inputs: `label` (string), `color` (enum: `default`, `success`, `warning`, `danger`).
*   **FR-18**: THE SYSTEM MUST render the badge as a small, rounded inline element with text content and background color derived from the `color` input.

#### 2.5 ValidationMessageComponent (`ui-validation-message`)
*   **FR-19**: THE SYSTEM MUST provide a `ui-validation-message` component with inputs: `message` (string), `type` (enum: `error`, `warning`, `info`).
*   **FR-20**: THE SYSTEM MUST render the message with appropriate icon and color based on the `type` input.

### 3. Molecule Components

#### 3.1 NameInputRowComponent (`ui-name-input-row`)
*   **FR-21**: THE SYSTEM MUST provide a `ui-name-input-row` composing `ui-input-field` and `ui-icon-button` (send arrow).
*   **FR-22**: THE SYSTEM MUST emit a `nameSubmitted` event when the user presses Enter or clicks the send button, passing the validated input value.
*   **FR-23**: IF the input is empty or contains only whitespace, THEN THE SYSTEM MUST prevent the `nameSubmitted` event from firing.

#### 3.2 ListCardComponent (`ui-list-card`)
*   **FR-24**: THE SYSTEM MUST provide a `ui-list-card` component with inputs: `title` (string), `phase` (string), `memberCount` (number).
*   **FR-25**: THE SYSTEM MUST render a clickable card displaying the list title, a `ui-badge` for the current phase, and a member count indicator.
*   **FR-43**: THE SYSTEM MUST provide a `ui-list-card` component with optional inputs `invitationCode` (string) and `invitationsOpen` (boolean). WHEN `invitationsOpen` is `true`, THE SYSTEM MUST render an "Invitar" action that emits an `inviteClicked` output.

#### 3.3 PhaseIndicatorComponent (`ui-phase-indicator`)
*   **FR-26**: THE SYSTEM MUST provide a `ui-phase-indicator` component with inputs: `phase` (string), `totalPhases` (number), `currentPhase` (number).
*   **FR-27**: THE SYSTEM MUST render a horizontal step indicator showing the progression through phases, highlighting the current phase.

#### 3.4 RoundIndicatorComponent (`ui-round-indicator`)
*   **FR-28**: THE SYSTEM MUST provide a `ui-round-indicator` component with inputs: `currentRound` (number), `totalRounds` (number).
*   **FR-29**: THE SYSTEM MUST render a compact display showing "Ronda X de Y" with a progress bar.

#### 3.5 CopyFieldComponent (`ui-copy-field`)
*   **FR-44**: THE SYSTEM MUST provide a `ui-copy-field` component with inputs: `label` (string), `value` (string).
*   **FR-45**: WHEN the Participant activates the "Copiar" action, THE SYSTEM MUST copy `value` to the clipboard and emit a `copied` output.
*   **FR-46**: IF the clipboard API is unavailable or the copy fails, THEN THE SYSTEM MUST select the displayed `value` for manual copying and emit a `copyFailed` output.

#### 3.6 InviteModalComponent (`ui-invite-modal`)
*   **FR-48**: THE SYSTEM MUST provide a `ui-invite-modal` molecule composing `ui-modal`, `ui-copy-field` and `ui-validation-message`, with inputs: `title` (string — invitation title, e.g. "Invitar a {list name}"), `code` (string), `visible` (boolean), and a `closed` output.
*   **FR-49**: WHEN the Participant activates "Copiar", THE SYSTEM MUST display the confirmation message ("Código copiado").
*   **FR-50**: IF the clipboard copy fails or is unavailable, THEN THE SYSTEM MUST display the manual-copy message ("No se ha podido copiar automáticamente. Copia el código manualmente.").

### 4. Organism Components

#### 4.1 DraggableRankingListComponent (`ui-draggable-ranking-list`)
*   **FR-30**: THE SYSTEM MUST provide a `ui-draggable-ranking-list` component with inputs: `items` (string[]), `disabled` (boolean).
*   **FR-31**: THE SYSTEM MUST use Angular CDK `cdkDrag` directives to enable reordering via drag-and-drop.
*   **FR-32**: THE SYSTEM MUST emit a `rankingsChanged` event with the reordered array after each drag operation.
*   **FR-33**: IF `disabled` is true, THEN THE SYSTEM MUST prevent drag interactions while keeping the visual list intact.
*   **FR-34**: THE SYSTEM MUST provide visual feedback during drag: the dragged item lifts with a shadow, and the drop zone highlights.

#### 4.2 ModalComponent (`ui-modal`)
*   **FR-35**: THE SYSTEM MUST provide a `ui-modal` component with inputs: `title` (string), `visible` (boolean).
*   **FR-36**: THE SYSTEM MUST emit a `closed` event when the user clicks the close button or presses Escape.
*   **FR-37**: THE SYSTEM MUST trap focus within the modal when it is visible and restore focus to the trigger element when closed.
*   **FR-38**: THE SYSTEM MUST render a backdrop overlay that prevents interaction with content behind the modal.

#### 4.3 AppBarComponent (`ui-app-bar`)
*   **FR-39**: THE SYSTEM MUST provide a `ui-app-bar` component with inputs: `title` (string), `logoutLabel` (string).
*   **FR-40**: THE SYSTEM MUST emit a `titleClicked` event when the title is activated and a `logoutClicked` event when the logout action is activated.
*   **FR-41**: THE SYSTEM MUST render the title on the left and the logout action on the right as a presentational bar with no routing or authentication logic.
*   **FR-47**: THE SYSTEM MUST provide a `ui-app-bar` component with optional inputs `userLabel` (string) and `showAdmin` (boolean), and an `adminClicked` output. WHEN `showAdmin` is `true`, THE SYSTEM MUST render an admin action that emits `adminClicked` when activated.

### 5. Theming & Responsiveness

*   **FR-51**: THE SYSTEM MUST render the dark palette by default. IF a consuming application does not override the design tokens, THEN THE SYSTEM MUST fall back to the dark values defined in `_variables.scss` with `color-scheme: dark`.
*   **FR-52**: EVERY responsive rule MUST use the breakpoint tokens `--ui-breakpoint-sm` (`640px`), `768px` (medium) and `1024px` (large), applied as `@media (min-width: ...)` for progressive enhancement.
*   **FR-53**: WHEN `ui-app-bar` is rendered in a viewport narrower than `640px`, THEN THE SYSTEM MUST allow the bar content to wrap and MUST hide the `userLabel` while keeping the title and every action (admin, logout) visible and operable.
*   **FR-54**: EVERY token table color MUST be a dark-palette-safe value that keeps a contrast ratio of at least 4.5:1 against its paired `on-*`/`container` surface. EVERY component MUST resolve colors exclusively through tokens (no hard-coded color values).

---

## Non-Functional Requirements
*   **NFR-1 (Independence)**: The `ui-kit` library MUST NOT import anything from the main application. It depends only on `@angular/core`, `@angular/common`, `@angular/material`, and `@angular/cdk`.
*   **NFR-2 (Language)**: All code, variable names, comments, and documentation are in English. Placeholder text in Storybook demos is in Spanish.
*   **NFR-3 (Accessibility)**: All interactive components MUST be keyboard-navigable and screen-reader accessible. Buttons must have `aria-label` when icon-only. Inputs must have associated labels via `aria-labelledby` or `for`/`id`.
*   **NFR-4 (Performance)**: All components MUST use `ChangeDetectionStrategy.OnPush`. Components MUST NOT perform HTTP requests or subscribe to external observables.
*   **NFR-5 (Testing)**: Every component MUST have a corresponding `.spec.ts` file with a minimum of 2 tests: (1) component renders without errors, (2) primary interaction fires the expected output event.
*   **NFR-6 (Consistency)**: All component inputs MUST use camelCase. All component selectors MUST use the `ui-` prefix with kebab-case (e.g., `ui-draggable-ranking-list`).

---

## Edge Cases
*   **Empty Items Array in DraggableRankingListComponent**: THE SYSTEM MUST render an empty container with no drag handles.
*   **Very Long Name in Input**: THE SYSTEM MUST truncate the displayed text with an ellipsis if it exceeds the container width. The full value remains available via the `value` output.
*   **Rapid Sequential Submissions in NameInputRow**: THE SYSTEM MUST debounce or disable the submit action for 300ms after each submission to prevent duplicate emissions.
*   **Modal Opened While Another Modal Is Open**: THE SYSTEM MUST stack modals (newest on top) and only trap focus within the topmost modal.
*   **Theme Tokens Not Defined**: IF a consuming application does not define the CSS custom properties, THEN THE SYSTEM MUST fall back to the default **dark** values defined in `_variables.scss`.
*   **Dark Palette on Light Systems**: IF the operating system prefers a light color scheme but the consuming application does not override the tokens, THEN THE SYSTEM MUST still render the dark palette (dark is the default, not system-driven).
*   **Clipboard Unavailable in CopyField**: `ui-copy-field` selects its `value` for manual copy and emits `copyFailed` (FR-46).
*   **Password Visibility Toggle Accessibility**: The toggle MUST keep focus after activation and announce its state via `aria-pressed`/`aria-label` (FR-42).
*   **Very Long Invitation Code Display**: `ui-copy-field` MUST truncate an over-long displayed value with an ellipsis while keeping the full value for copy.

---

## Out of Scope
*   Real-time collaborative editing within components.
*   Form validation logic (components emit raw values; validation belongs to the consuming application).
*   Internationalization (i18n) of component labels (use `@Input()` for all text).
*   Server-side rendering (SSR) support.
*   Theming beyond CSS custom properties (no runtime theme switching service).

---

## Initial Test Scenarios (TDD Requirement)
*   **TS-1**: `ui-button` renders label text and emits `clicked` on click.
*   **TS-2**: `ui-button` with `loading=true` shows spinner and disables interaction.
*   **TS-3**: `ui-input-field` emits `valueChanged` on input and `submitted` on Enter.
*   **TS-4**: `ui-input-field` with `error` set displays red border and error message.
*   **TS-5**: `ui-icon-button` emits `clicked` on activation.
*   **TS-6**: `ui-badge` renders correct background color for each `color` variant.
*   **TS-7**: `ui-validation-message` renders correct icon and color for `error`, `warning`, and `info` types.
*   **TS-8**: `ui-name-input-row` emits `nameSubmitted` with the trimmed value on Enter.
*   **TS-9**: `ui-name-input-row` prevents `nameSubmitted` when input is empty or whitespace-only.
*   **TS-10**: `ui-list-card` renders title, phase badge, and member count.
*   **TS-11**: `ui-phase-indicator` highlights the correct current phase.
*   **TS-12**: `ui-round-indicator` displays "Ronda X de Y" text.
*   **TS-13**: `ui-draggable-ranking-list` emits `rankingsChanged` with reordered array after drag.
*   **TS-14**: `ui-draggable-ranking-list` with `disabled=true` prevents drag interaction.
*   **TS-15**: `ui-modal` emits `closed` on close button click.
*   **TS-16**: `ui-modal` traps focus when visible and restores focus when closed.
*   **TS-17**: All atom components render without errors when minimal inputs are provided.
*   **TS-18**: `ui-app-bar` renders the title and logout label, and emits `titleClicked`/`logoutClicked` on activation.
*   **TS-19**: `ui-input-field` with `type="password"` renders a visibility toggle; activating it flips the input type and `aria-pressed`.
*   **TS-20**: `ui-list-card` with `invitationsOpen=true` renders "Invitar" and emits `inviteClicked`; with `invitationsOpen=false` it does not.
*   **TS-21**: `ui-app-bar` with `showAdmin=true` renders the admin action and emits `adminClicked` on activation.
*   **TS-22**: `ui-copy-field` copies `value` on "Copiar" and emits `copied`; with an unavailable clipboard it selects the value and emits `copyFailed`.
*   **TS-23**: `ui-invite-modal` renders the title, code and copy action; it emits `closed` on close and shows the copied/manual-copy notices per FR-49/FR-50.
*   **TS-24**: `_variables.scss` defines the dark palette by default (paired `on-*`/`container` tokens with ≥4.5:1 contrast) plus `--ui-color-danger-hover`, and sets `color-scheme: dark`.
*   **TS-25**: `ui-app-bar` keeps the title and the admin/logout actions in the DOM under `640px` while dropping the `userLabel` from view (CSS breakpoint rule per FR-53).

---

## Completion Criteria
*   `ui-kit` directory structure created with atoms, molecules, organisms, and tokens.
*   Design token SCSS file (`_variables.scss`) with all variables defined and documented.
*   All atom components implemented (Button, InputField, IconButton, Badge, ValidationMessage) with unit tests.
*   All molecule components implemented (NameInputRow, ListCard, PhaseIndicator, RoundIndicator) with unit tests.
*   All organism components implemented (DraggableRankingList, Modal, AppBar) with unit tests.
*   The `ui-copy-field` molecule implemented with clipboard fallback (FR-44..FR-46) and unit tests.
*   Input password visibility toggle, list-card invitation action, and app-bar admin action implemented (FR-42, FR-43, FR-47) with unit tests.
*   Full test suite passing (`npm test`) with zero lint warnings (`npm run lint`).
*   Components usable from the main application via direct import (no barrel-export configuration required for in-project use).
