import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';

import { UiIconButtonComponent } from '../../atoms/icon-button/icon-button.component';
import { UiInputFieldComponent } from '../../atoms/input-field/input-field.component';

const SUBMIT_LOCK_MS = 300;

@Component({
  selector: 'ui-name-input-row',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiInputFieldComponent, UiIconButtonComponent],
  template: `
    <div class="ui-name-input-row">
      <ui-input-field
        class="ui-name-input-row__field"
        [label]="label()"
        [placeholder]="placeholder()"
        [value]="currentValue()"
        [disabled]="disabled() || submitting()"
        [maxLength]="maxLength()"
        (valueChanged)="onValueChanged($event)"
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
export class UiNameInputRowComponent {
  readonly label = input('');
  readonly placeholder = input('');
  readonly value = input('');
  readonly disabled = input<boolean>(false);
  readonly maxLength = input<number>();
  readonly nameSubmitted = output<string>();

  private readonly typedValue = signal('');
  private readonly submitting = signal(false);
  private readonly destroyRef = inject(DestroyRef);
  private submitTimer: ReturnType<typeof setTimeout> | undefined;

  readonly currentValue = computed(() => this.typedValue() || this.value());

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.submitTimer) {
        clearTimeout(this.submitTimer);
      }
    });
  }

  onValueChanged(value: string): void {
    this.typedValue.set(value);
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
    this.submitting.set(true);
    this.submitTimer = setTimeout(() => {
      this.submitting.set(false);
      this.submitTimer = undefined;
    }, SUBMIT_LOCK_MS);
  }
}