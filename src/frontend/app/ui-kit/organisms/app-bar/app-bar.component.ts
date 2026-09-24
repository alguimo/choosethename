import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { UiButtonComponent } from '../../atoms/button/button.component';

@Component({
  selector: 'ui-app-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButtonComponent],
  template: `
    <header class="ui-app-bar">
      <button type="button" class="ui-app-bar__title" (click)="titleClicked.emit()">
        {{ title() }}
      </button>
      <div class="ui-app-bar__actions">
        @if (userLabel()) {
          <span class="ui-app-bar__user">{{ userLabel() }}</span>
        }
        @if (showAdmin()) {
          <ui-button
            variant="ghost"
            label="Admin"
            (clicked)="adminClicked.emit()"
          ></ui-button>
        }
        <ui-button
          variant="ghost"
          [label]="logoutLabel()"
          (clicked)="logoutClicked.emit()"
        ></ui-button>
      </div>
    </header>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .ui-app-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--ui-spacing-md);
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border-bottom: 1px solid var(--ui-color-outline-variant);
        background-color: var(--ui-color-surface);
        font-family: var(--ui-font-family);
      }

      .ui-app-bar__title {
        padding: 0;
        border: none;
        background: none;
        font-family: inherit;
        font-size: var(--ui-font-size-lg);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
        cursor: pointer;
      }

      .ui-app-bar__title:focus-visible {
        outline: 2px solid var(--ui-color-focus-ring);
        outline-offset: 2px;
      }

      .ui-app-bar__actions {
        display: flex;
        align-items: center;
        gap: var(--ui-spacing-xs);
      }

      .ui-app-bar__user {
        margin-right: var(--ui-spacing-xs);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface-variant);
      }

      @media (max-width: 640px) {
        .ui-app-bar {
          flex-wrap: wrap;
        }

        .ui-app-bar__user {
          display: none;
        }
      }
    `,
  ],
})
export class UiAppBarComponent {
  readonly title = input('');
  readonly logoutLabel = input('');
  readonly userLabel = input('');
  readonly showAdmin = input(false);
  readonly titleClicked = output<void>();
  readonly logoutClicked = output<void>();
  readonly adminClicked = output<void>();
}
