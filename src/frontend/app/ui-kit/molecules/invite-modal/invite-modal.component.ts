import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';

import { UiValidationMessageComponent } from '../../atoms/validation-message/validation-message.component';
import { UiButtonComponent } from '../../atoms/button/button.component';
import { UiCopyFieldComponent } from '../copy-field/copy-field.component';
import { UiModalComponent } from '../../organisms/modal/modal.component';

@Component({
  selector: 'ui-invite-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiModalComponent, UiCopyFieldComponent, UiButtonComponent, UiValidationMessageComponent],
  template: `
    <ui-modal [title]="title()" [visible]="visible()" (closed)="onClosed()">
      <p class="ui-invite-modal__hint">
        Comparte este código con otra persona para que pueda unirse a la lista.
      </p>
      <ui-copy-field
        label="Código de invitación"
        [value]="code()"
        (copied)="onCopied()"
        (copyFailed)="onCopyFailed()"
      ></ui-copy-field>
      @if (notice()) {
        <ui-validation-message [type]="copySucceeded() ? 'info' : 'warning'" [message]="notice()!" />
      }
      <div class="ui-invite-modal__actions">
        <ui-button label="Cerrar" variant="ghost" (clicked)="onClosed()"></ui-button>
      </div>
    </ui-modal>
  `,
  styles: [
    `
      .ui-invite-modal__hint {
        margin: 0 0 var(--ui-spacing-sm);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface-variant);
      }

      .ui-invite-modal__actions {
        display: flex;
        justify-content: flex-end;
        gap: var(--ui-spacing-sm);
        margin-top: var(--ui-spacing-md);
      }
    `,
  ],
})
export class UiInviteModalComponent {
  readonly title = input('');
  readonly code = input('');
  readonly visible = input<boolean>(false);
  readonly closed = output<void>();

  protected readonly copySucceeded = signal(false);
  protected readonly notice = signal<string | null>(null);

  constructor() {
    effect(
      () => {
        if (this.visible()) {
          this.copySucceeded.set(false);
          this.notice.set(null);
        }
      },
      { allowSignalWrites: true },
    );
  }

  onClosed(): void {
    this.closed.emit();
  }

  onCopied(): void {
    this.copySucceeded.set(true);
    this.notice.set('Código copiado');
  }

  onCopyFailed(): void {
    this.copySucceeded.set(false);
    this.notice.set('No se ha podido copiar automáticamente. Copia el código manualmente.');
  }
}