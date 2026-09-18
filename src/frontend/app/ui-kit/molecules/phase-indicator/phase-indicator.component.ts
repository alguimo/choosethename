import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { phaseLabel } from '../../phase-presentation';

@Component({
  selector: 'ui-phase-indicator',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav
      class="ui-phase-indicator"
      role="navigation"
      aria-label="Indicador de fase"
    >
      <ol class="ui-phase-indicator__steps">
        @for (step of steps(); track step; let i = $index) {
          <li
            class="ui-phase-indicator__step"
            [class.ui-phase-indicator__step--completed]="step < currentPhase()"
            [class.ui-phase-indicator__step--current]="step === currentPhase()"
          >
            <span
              class="ui-phase-indicator__circle"
              [attr.aria-current]="step === currentPhase() ? 'step' : null"
            >{{ step }}</span>
            @if (step === currentPhase()) {
              <span class="ui-phase-indicator__label">{{ label() }}</span>
            }
          </li>
          @if (i < steps().length - 1) {
            <li
              class="ui-phase-indicator__line"
              [class.ui-phase-indicator__line--active]="step < currentPhase()"
              aria-hidden="true"
            ></li>
          }
        }
      </ol>
    </nav>
  `,
  styles: [
    `
      .ui-phase-indicator__steps {
        display: flex;
        align-items: flex-start;
        gap: 0;
        margin: 0;
        padding: 0;
        list-style: none;
      }

      .ui-phase-indicator__step {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--ui-spacing-xs);
        position: relative;
      }

      .ui-phase-indicator__circle {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 28px;
        height: 28px;
        border-radius: var(--ui-radius-full);
        background-color: var(--ui-color-surface-variant);
        color: var(--ui-color-on-surface-variant);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        font-weight: var(--ui-font-weight-medium);
        border: 2px solid transparent;
      }

      .ui-phase-indicator__step--completed .ui-phase-indicator__circle {
        background-color: var(--ui-color-success);
        color: var(--ui-color-on-success);
      }

      .ui-phase-indicator__step--current .ui-phase-indicator__circle {
        background-color: var(--ui-color-primary);
        color: var(--ui-color-on-primary);
        border-color: var(--ui-color-focus-ring);
      }

      .ui-phase-indicator__label {
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xs);
        color: var(--ui-color-on-surface-variant);
        white-space: nowrap;
      }

      .ui-phase-indicator__line {
        flex: 1;
        align-self: center;
        width: 100%;
        height: 2px;
        min-width: var(--ui-spacing-lg);
        margin-top: 13px;
        background-color: var(--ui-color-outline-variant);
      }

      .ui-phase-indicator__line--active {
        background-color: var(--ui-color-success);
      }
    `,
  ],
})
export class UiPhaseIndicatorComponent {
  readonly phase = input('');
  readonly totalPhases = input<number>(1);
  readonly currentPhase = input<number>(1);

  readonly steps = computed(() =>
    Array.from({ length: this.totalPhases() }, (_, i) => i + 1),
  );

  readonly label = computed(() => phaseLabel(this.phase()));
}