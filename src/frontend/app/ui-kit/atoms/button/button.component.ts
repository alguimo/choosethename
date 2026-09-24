import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
  selector: 'ui-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatProgressSpinner],
  template: `
    <button
      class="ui-button"
      [class.ui-button--secondary]="variant() === 'secondary'"
      [class.ui-button--danger]="variant() === 'danger'"
      [class.ui-button--ghost]="variant() === 'ghost'"
      [disabled]="disabled() || loading()"
      (click)="handleClick()"
      (keydown.enter)="handleClick()"
    >
      @if (loading()) {
        <mat-spinner class="ui-button__spinner" [diameter]="20"></mat-spinner>
      }
      <span class="ui-button__label">{{ label() }}</span>
    </button>
  `,
  styles: [
    `
      :host {
        display: inline-block;
      }

      .ui-button {
        display: inline-flex;
        align-items: center;
        gap: var(--ui-spacing-sm);
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid transparent;
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-primary);
        color: var(--ui-color-on-primary);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        font-weight: var(--ui-font-weight-medium);
        line-height: var(--ui-line-height-tight);
        cursor: pointer;
        transition:
          background-color var(--ui-transition-fast),
          border-color var(--ui-transition-fast),
          opacity var(--ui-transition-fast);
      }

      .ui-button:hover:not(:disabled) {
        background-color: var(--ui-color-primary-hover);
      }

      .ui-button:focus-visible {
        outline: 2px solid var(--ui-color-focus-ring);
        outline-offset: 2px;
      }

      .ui-button:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .ui-button--secondary {
        background-color: transparent;
        color: var(--ui-color-primary);
        border-color: var(--ui-color-outline);
      }

      .ui-button--secondary:hover:not(:disabled) {
        background-color: var(--ui-color-primary-container);
      }

      .ui-button--danger {
        background-color: var(--ui-color-danger);
        color: var(--ui-color-on-danger);
      }

      .ui-button--danger:hover:not(:disabled) {
        background-color: var(--ui-color-danger-hover);
      }

      .ui-button--ghost {
        background-color: transparent;
        color: var(--ui-color-on-surface);
      }

      .ui-button--ghost:hover:not(:disabled) {
        background-color: var(--ui-color-surface-variant);
      }

      .ui-button__spinner ::ng-deep circle {
        stroke: currentColor;
      }
    `,
  ],
})
export class UiButtonComponent {
  readonly label = input('');
  readonly variant = input<'primary' | 'secondary' | 'danger' | 'ghost'>('primary');
  readonly disabled = input<boolean>(false);
  readonly loading = input<boolean>(false);
  readonly clicked = output<void>();

  handleClick(): void {
    if (!this.disabled() && !this.loading()) {
      this.clicked.emit();
    }
  }
}