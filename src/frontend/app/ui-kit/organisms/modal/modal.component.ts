import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  HostListener,
  inject,
  input,
  output,
} from '@angular/core';
import { CdkTrapFocus } from '@angular/cdk/a11y';

import { UiIconButtonComponent } from '../../atoms/icon-button/icon-button.component';

@Component({
  selector: 'ui-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    UiIconButtonComponent,
    CdkTrapFocus,
  ],
  template: `
    @if (visible()) {
      <button
        type="button"
        class="ui-modal-backdrop"
        [attr.aria-label]="'Cerrar ' + (title() || 'modal')"
        (click)="onClose()"
      ></button>
      <div
        class="ui-modal-panel"
        role="dialog"
        aria-modal="true"
        [attr.aria-labelledby]="titleId"
        cdkTrapFocus
        tabindex="-1"
      >
        <header class="ui-modal-panel__header">
          <h2 [id]="titleId" class="ui-modal-panel__title">{{ title() }}</h2>
          <ui-icon-button icon="close" tooltip="Cerrar" (clicked)="onClose()"></ui-icon-button>
        </header>
        <div class="ui-modal-panel__body">
          <ng-content></ng-content>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .ui-modal-backdrop {
        position: fixed;
        inset: 0;
        padding: 0;
        border: none;
        background-color: var(--ui-color-backdrop);
        z-index: var(--ui-z-index-modal);
      }

      .ui-modal-panel {
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: min(90vw, 480px);
        max-height: 85vh;
        overflow-y: auto;
        border-radius: var(--ui-radius-md);
        background-color: var(--ui-color-surface);
        box-shadow: var(--ui-shadow-lg);
        z-index: calc(var(--ui-z-index-modal) + 1);
        font-family: var(--ui-font-family);
      }

      .ui-modal-panel__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--ui-spacing-sm);
        padding: var(--ui-spacing-md) var(--ui-spacing-md) 0;
      }

      .ui-modal-panel__title {
        margin: 0;
        font-size: var(--ui-font-size-lg);
        font-weight: var(--ui-font-weight-semibold);
        line-height: var(--ui-line-height-tight);
        color: var(--ui-color-on-surface);
      }

      .ui-modal-panel__body {
        padding: var(--ui-spacing-md);
        color: var(--ui-color-on-surface);
      }
    `,
  ],
})
export class UiModalComponent {
  private static readonly baseId = 'ui-modal';

  private static instanceCounter = 0;

  private readonly uid = ++UiModalComponent.instanceCounter;

  private readonly destroyRef = inject(DestroyRef);

  private triggerElement: HTMLElement | null = null;

  private readonly trackFocus = (event: FocusEvent): void => {
    if (!this.visible()) {
      this.triggerElement = event.target as HTMLElement;
    }
  };

  readonly titleId = `${UiModalComponent.baseId}-title-${this.uid}`;

  readonly title = input('');
  readonly visible = input<boolean>(false);
  readonly closed = output<void>();

  constructor() {
    document.addEventListener('focusin', this.trackFocus);
    this.destroyRef.onDestroy(() => {
      document.removeEventListener('focusin', this.trackFocus);
    });

    effect(() => {
      if (!this.visible()) {
        this.triggerElement?.focus();
        this.triggerElement = null;
      }
    });
  }

  onClose(): void {
    this.closed.emit();
  }

  @HostListener('keydown.escape')
  onEscape(): void {
    if (this.visible()) {
      this.closed.emit();
    }
  }
}