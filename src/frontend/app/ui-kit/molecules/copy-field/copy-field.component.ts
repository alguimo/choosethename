import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';
import { UiButtonComponent } from '../../atoms/button/button.component';

@Component({
  selector: 'ui-copy-field',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiButtonComponent],
  template: `
    <div class="ui-copy-field">
      @if (label()) {
        <span class="ui-copy-field__label">{{ label() }}</span>
      }
      <div class="ui-copy-field__row">
        <input
          #valueInput
          class="ui-copy-field__value"
          readonly
          [value]="value()"
          (focus)="onFocus($event)"
        />
        <ui-button label="Copiar" variant="secondary" (clicked)="copy()"></ui-button>
      </div>
    </div>
  `,
  styles: [
    `
      .ui-copy-field {
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-xs);
      }

      .ui-copy-field__label {
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        font-weight: var(--ui-font-weight-medium);
        color: var(--ui-color-on-surface-variant);
      }

      .ui-copy-field__row {
        display: flex;
        align-items: stretch;
        gap: var(--ui-spacing-sm);
      }

      .ui-copy-field__value {
        flex: 1;
        min-width: 0;
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline);
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-surface-variant);
        color: var(--ui-color-on-surface);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        line-height: var(--ui-line-height-normal);
      }
    `,
  ],
})
export class UiCopyFieldComponent {
  readonly label = input('');
  readonly value = input('');
  readonly copied = output<void>();
  readonly copyFailed = output<void>();

  private readonly valueInput = viewChild<ElementRef<HTMLInputElement>>('valueInput');

  async copy(): Promise<void> {
    const target = this.value();

    if (navigator.clipboard?.writeText) {
      try {
        await navigator.clipboard.writeText(target);
        this.copied.emit();
        return;
      } catch {
        this.fallback();
        return;
      }
    }

    this.fallback();
  }

  onFocus(event: Event): void {
    (event.target as HTMLInputElement).select();
  }

  private fallback(): void {
    this.valueInput()?.nativeElement.select();
    this.copyFailed.emit();
  }
}