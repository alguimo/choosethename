import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type UiBadgeColor = 'default' | 'success' | 'warning' | 'danger';

@Component({
  selector: 'ui-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="ui-badge"
      [class.ui-badge--success]="color() === 'success'"
      [class.ui-badge--warning]="color() === 'warning'"
      [class.ui-badge--danger]="color() === 'danger'"
    >
      {{ label() }}
    </span>
  `,
  styles: [
    `
      .ui-badge {
        display: inline-flex;
        align-items: center;
        padding: var(--ui-spacing-xs) var(--ui-spacing-sm);
        border-radius: var(--ui-radius-full);
        background-color: var(--ui-color-surface-variant);
        color: var(--ui-color-on-surface-variant);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xs);
        font-weight: var(--ui-font-weight-medium);
        line-height: var(--ui-line-height-tight);
        letter-spacing: var(--ui-letter-spacing-normal);
      }

      .ui-badge--success {
        background-color: var(--ui-color-success-container);
        color: var(--ui-color-on-success-container);
      }

      .ui-badge--warning {
        background-color: var(--ui-color-warning-container);
        color: var(--ui-color-on-warning-container);
      }

      .ui-badge--danger {
        background-color: var(--ui-color-danger-container);
        color: var(--ui-color-on-danger-container);
      }
    `,
  ],
})
export class UiBadgeComponent {
  readonly label = input('');
  readonly color = input<UiBadgeColor>('default');
}