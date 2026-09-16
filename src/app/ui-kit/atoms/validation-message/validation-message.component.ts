import { ChangeDetectionStrategy, Component, computed, HostBinding, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

export type UiValidationType = 'error' | 'warning' | 'info';

const ICON_BY_TYPE: Record<UiValidationType, string> = {
  error: 'error',
  warning: 'warning',
  info: 'info',
};

const ROLE_BY_TYPE: Record<UiValidationType, 'alert' | 'status'> = {
  error: 'alert',
  warning: 'status',
  info: 'status',
};

@Component({
  selector: 'ui-validation-message',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon],
  template: `
    <div
      class="ui-validation-message"
      [class.ui-validation-message--error]="type() === 'error'"
      [class.ui-validation-message--warning]="type() === 'warning'"
      [class.ui-validation-message--info]="type() === 'info'"
    >
      <mat-icon class="ui-validation-message__icon">{{ icon() }}</mat-icon>
      <span class="ui-validation-message__text">{{ message() }}</span>
    </div>
  `,
  styles: [
    `
      .ui-validation-message {
        display: flex;
        align-items: flex-start;
        gap: var(--ui-spacing-xs);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        line-height: var(--ui-line-height-normal);
      }

      .ui-validation-message--error {
        color: var(--ui-color-danger);
      }

      .ui-validation-message--warning {
        color: var(--ui-color-warning);
      }

      .ui-validation-message--info {
        color: var(--ui-color-info);
      }

      .ui-validation-message__icon {
        width: 16px;
        height: 16px;
        font-size: 16px;
        line-height: 16px;
        flex-shrink: 0;
      }
    `,
  ],
})
export class UiValidationMessageComponent {
  readonly message = input('');
  readonly type = input<UiValidationType>('error');
  readonly role = computed(() => ROLE_BY_TYPE[this.type()]);
  readonly icon = computed(() => ICON_BY_TYPE[this.type()]);

  @HostBinding('attr.role') get hostRole(): 'alert' | 'status' {
    return this.role();
  }
}