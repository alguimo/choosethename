import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'ui-icon-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon],
  template: `
    <button
      class="ui-icon-button"
      [class.ui-icon-button--danger]="variant() === 'danger'"
      [disabled]="disabled()"
      [attr.aria-label]="tooltip() || null"
      (click)="handleClick()"
      (keydown.enter)="handleClick()"
    >
      <mat-icon>{{ icon() }}</mat-icon>
    </button>
  `,
  styles: [
    `
      :host {
        display: inline-block;
        line-height: 0;
      }

      .ui-icon-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        padding: 0;
        border: none;
        border-radius: var(--ui-radius-full);
        background-color: transparent;
        color: var(--ui-color-on-surface);
        cursor: pointer;
        transition: background-color var(--ui-transition-fast);
      }

      .ui-icon-button:hover:not(:disabled) {
        background-color: var(--ui-color-surface-variant);
      }

      .ui-icon-button:focus-visible {
        outline: 2px solid var(--ui-color-focus-ring);
        outline-offset: 2px;
      }

      .ui-icon-button:disabled {
        opacity: 0.38;
        cursor: not-allowed;
      }

      .ui-icon-button--danger {
        color: var(--ui-color-danger);
      }

      .ui-icon-button--danger:hover:not(:disabled) {
        background-color: var(--ui-color-danger-container);
      }

      .ui-icon-button mat-icon {
        width: 20px;
        height: 20px;
        font-size: 20px;
        line-height: 20px;
      }
    `,
  ],
})
export class UiIconButtonComponent {
  readonly icon = input('');
  readonly tooltip = input('');
  readonly disabled = input<boolean>(false);
  readonly variant = input<'default' | 'danger'>('default');
  readonly clicked = output<void>();

  handleClick(): void {
    if (!this.disabled()) {
      this.clicked.emit();
    }
  }
}