import {
  ChangeDetectionStrategy,
  Component,
  ChangeDetectorRef,
  computed,
  inject,
  input,
  output,
  signal,
  forwardRef,
} from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'ui-input-field',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiInputFieldComponent),
      multi: true
    }
  ],
  template: `
    <div class="ui-input-field">
      @if (label()) {
        <label [for]="inputId" class="ui-input-field__label">{{ label() }}</label>
      }
      <div class="ui-input-field__control-wrapper">
        <input
          [id]="inputId"
          class="ui-input-field__control"
          [class.ui-input-field__control--error]="error()"
          [class.ui-input-field__control--password]="isPassword()"
          [placeholder]="placeholder()"
          [type]="effectiveType()"
          [value]="value"
          [disabled]="disabled() || isDisabled"
          [attr.maxlength]="maxLength() ?? null"
          [attr.aria-invalid]="error() ? 'true' : null"
          [attr.aria-describedby]="error() ? errorId : null"
          (input)="onInput($event)"
          (blur)="onTouched()"
          (keydown.enter)="onSubmit($event)"
        />
        @if (isPassword()) {
          <button
            type="button"
            class="ui-input-field__toggle"
            [attr.aria-pressed]="visible()"
            [attr.aria-label]="eyeAriaLabel()"
            [attr.aria-controls]="inputId"
            (click)="toggleVisibility()"
          >
            <mat-icon>{{ visible() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        }
      </div>
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
        transition: border-color var(--ui-transition-fast), box-shadow var(--ui-transition-fast);
      }

      .ui-input-field__control-wrapper {
        position: relative;
        display: block;
      }

      .ui-input-field__control--password {
        padding-right: calc(var(--ui-spacing-md) + 40px);
      }

.ui-input-field__toggle {
        position: absolute;
        top: 50%;
        right: var(--ui-spacing-xs);
        transform: translateY(-50%);
        display: grid;
        place-items: center;
        width: 40px;
        height: 40px;
        padding: 0;
        border: none;
        border-radius: var(--ui-radius-full);
        background-color: transparent;
        color: var(--ui-color-on-surface-variant);
        cursor: pointer;
      }

.ui-input-field__toggle:hover:not(:disabled) {
        background-color: var(--ui-color-surface-variant);
      }

      .ui-input-field__toggle:focus-visible {
        outline: 2px solid var(--ui-color-focus-ring);
        outline-offset: -1px;
      }

      .ui-input-field__toggle mat-icon {
        width: 20px;
        font-size: 20px;
      }

      .ui-input-field__control::placeholder {
        color: var(--ui-color-on-surface-variant);
      }

      .ui-input-field__control:focus {
        outline: none;
        border-color: var(--ui-color-primary);
        box-shadow: 0 0 0 1px var(--ui-color-primary);
      }

      .ui-input-field__control:disabled {
        background-color: var(--ui-color-surface-variant);
        cursor: not-allowed;
      }

      .ui-input-field__control--error {
        border-color: var(--ui-color-danger);
      }

      .ui-input-field__control--error:focus {
        border-color: var(--ui-color-danger);
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
export class UiInputFieldComponent implements ControlValueAccessor {
  private static readonly baseId = 'ui-input-field';
  private static instanceCounter = 0;
  private readonly uid = ++UiInputFieldComponent.instanceCounter;
  readonly inputId = `${UiInputFieldComponent.baseId}-${this.uid}`;
  readonly errorId = `${UiInputFieldComponent.baseId}-error-${this.uid}`;

  readonly label = input('');
  readonly placeholder = input('');
  readonly type = input<'text' | 'password'>('text');
  readonly error = input('');
  readonly disabled = input<boolean>(false);
  readonly maxLength = input<number>();
  readonly valueChanged = output<string>();
  readonly submitted = output<void>();

  private readonly cdr = inject(ChangeDetectorRef);
  value = '';
  isDisabled: boolean = false;

  readonly visible = signal(false);
  readonly isPassword = computed(() => this.type() === 'password');
  readonly effectiveType = computed(() =>
    this.isPassword() && this.visible() ? 'text' : this.type(),
  );
  readonly eyeAriaLabel = computed(() =>
    this.visible() ? 'Ocultar contraseña' : 'Mostrar contraseña',
  );

  toggleVisibility(): void {
    this.visible.update((state) => !state);
  }

  onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.value = value;
    this.onChange(value);
    this.valueChanged.emit(value);
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    this.submitted.emit();
  }

  writeValue(value: string): void {
    this.value = value;
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    this.cdr.markForCheck();
  }
}
