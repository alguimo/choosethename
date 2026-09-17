import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  output,
  signal,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { UiIconButtonComponent } from '../../atoms/icon-button/icon-button.component';
import { UiInputFieldComponent } from '../../atoms/input-field/input-field.component';

const SUBMIT_LOCK_MS = 300;

@Component({
  selector: 'ui-name-input-row',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInputFieldComponent, UiIconButtonComponent, FormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiNameInputRowComponent),
      multi: true
    }
  ],
  template: `
    <div class="ui-name-input-row">
      <ui-input-field
        class="ui-name-input-row__field"
        [label]="label()"
        [placeholder]="placeholder()"
        [ngModel]="currentValue()"
        (ngModelChange)="onValueChanged($event)"
        [disabled]="disabled() || submitting()"
        [maxLength]="maxLength()"
        (submitted)="onSubmit()"
      ></ui-input-field>
      <ui-icon-button
        class="ui-name-input-row__submit"
        icon="send"
        tooltip="Enviar"
        [disabled]="disabled() || submitting()"
        (clicked)="onSubmit()"
      ></ui-icon-button>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .ui-name-input-row {
        display: flex;
        align-items: flex-start;
        gap: var(--ui-spacing-sm);
      }

      .ui-name-input-row__field {
        flex: 1;
        min-width: 0;
      }

      .ui-name-input-row__submit {
        margin-top: var(--ui-spacing-lg);
        flex-shrink: 0;
      }
    `,
  ],
})
export class UiNameInputRowComponent implements ControlValueAccessor {
  readonly label = input('');
  readonly placeholder = input('');
  readonly disabled = input<boolean>(false);
  readonly maxLength = input<number>();
  readonly nameSubmitted = output<string>();

  private readonly typedValue = signal('');
  private readonly submitting = signal(false);
  private readonly destroyRef = inject(DestroyRef);
  private submitTimer: ReturnType<typeof setTimeout> | undefined;

  readonly currentValue = computed(() => this.typedValue());

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.submitTimer) {
        clearTimeout(this.submitTimer);
      }
    });
  }

  onValueChanged(value: string): void {
    this.typedValue.set(value);
    this.onChange(value);
    this.onTouched();
  }

  onSubmit(): void {
    if (this.disabled() || this.submitting()) {
      return;
    }

    const value = this.currentValue().trim();
    if (!value) {
      return;
    }

    this.nameSubmitted.emit(value);
    this.typedValue.set('');
    this.onChange('');
    this.submitting.set(true);
    this.submitTimer = setTimeout(() => {
      this.submitting.set(false);
      this.submitTimer = undefined;
    }, SUBMIT_LOCK_MS);
  }

  onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(value: string): void {
    this.typedValue.set(value);
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
}
