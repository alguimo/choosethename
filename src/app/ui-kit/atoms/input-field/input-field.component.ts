import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'ui-input-field',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ui-input-field">
      @if (label()) {
        <label [for]="inputId" class="ui-input-field__label">{{ label() }}</label>
      }
      <input
        [id]="inputId"
        class="ui-input-field__control"
        [class.ui-input-field__control--error]="error()"
        [placeholder]="placeholder()"
        [value]="value()"
        [disabled]="disabled()"
        [attr.maxlength]="maxLength() ?? null"
        [attr.aria-invalid]="error() ? 'true' : null"
        [attr.aria-describedby]="error() ? errorId : null"
        (input)="onInput($event)"
        (keydown.enter)="onSubmit($event)"
      />
      @if (error()) {
        <span [id]="errorId" class="ui-input-field__error">{{ error() }}</span>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .ui-input-field__label {
        display: block;
        margin-bottom: var(--ui-spacing-xs);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        font-weight: var(--ui-font-weight-medium);
        color: var(--ui-color-on-surface);
      }

      .ui-input-field__control {
        display: block;
        width: 100%;
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline);
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-surface);
        color: var(--ui-color-on-surface);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        line-height: var(--ui-line-height-normal);
        transition:
          border-color var(--ui-transition-fast),
          box-shadow var(--ui-transition-fast);
      }

      .ui-input-field__control::placeholder {
        color: var(--ui-color-on-surface-variant);
        opacity: 1;
      }

      .ui-input-field__control:focus {
        outline: none;
        border-color: var(--ui-color-primary);
        box-shadow: 0 0 0 1px var(--ui-color-primary);
      }

      .ui-input-field__control:disabled {
        background-color: var(--ui-color-surface-variant);
        opacity: 0.38;
        cursor: not-allowed;
      }

      .ui-input-field__control--error {
        border-color: var(--ui-color-danger);
      }

      .ui-input-field__control--error:focus {
        border-color: var(--ui-color-danger);
        box-shadow: 0 0 0 1px var(--ui-color-danger);
      }

      .ui-input-field__error {
        display: block;
        margin-top: var(--ui-spacing-xs);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-danger);
      }
    `,
  ],
})
export class UiInputFieldComponent {
  private static readonly baseId = 'ui-input-field';

  private static instanceCounter = 0;

  private readonly uid = ++UiInputFieldComponent.instanceCounter;

  readonly inputId = `${UiInputFieldComponent.baseId}-${this.uid}`;

  readonly errorId = `${UiInputFieldComponent.baseId}-error-${this.uid}`;

  readonly label = input('');
  readonly placeholder = input('');
  readonly value = input('');
  readonly error = input('');
  readonly disabled = input<boolean>(false);
  readonly maxLength = input<number>();
  readonly valueChanged = output<string>();
  readonly submitted = output<void>();

  onInput(event: Event): void {
    if (!this.disabled()) {
      this.valueChanged.emit((event.target as HTMLInputElement).value);
    }
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    this.submitted.emit();
  }
}