import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../services/api.service';
import { passwordComplexity } from '../../auth-validators';
import { ApiError, UserProfile } from '../../models/api.models';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiInputFieldComponent } from '../../ui-kit/atoms/input-field/input-field.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';
import { UiModalComponent } from '../../ui-kit/organisms/modal/modal.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    UiButtonComponent,
    UiInputFieldComponent,
    UiValidationMessageComponent,
    UiModalComponent,
  ],
  template: `
    <section class="admin">
      <h2 class="admin__title">Administración</h2>

      @if (successMessage()) {
        <ui-validation-message type="info" [message]="successMessage()!" />
      }

      @if (loadError()) {
        <ui-validation-message type="error" [message]="loadError()!" />
      }

      @if (loading()) {
        <p class="admin__loading">Cargando...</p>
      }

      @if (!loading() && !loadError() && users().length === 0) {
        <p class="admin__empty">No hay usuarios registrados</p>
      }

      <ul class="admin__users">
        @for (user of users(); track user.id) {
          <li class="admin__user">
            <span class="admin__user-info">
              <span class="admin__user-name">{{ user.username }}</span>
              <span class="admin__user-role">{{ user.role }}</span>
            </span>
            <ui-button
              variant="secondary"
              label="Restablecer contraseña"
              (clicked)="openReset(user)"
            ></ui-button>
          </li>
        }
      </ul>

      <ui-modal
        title="Restablecer contraseña"
        [visible]="resetTarget() !== null"
        (closed)="closeReset()"
      >
        <form [formGroup]="resetForm" (ngSubmit)="submitReset()">
          <p class="admin__reset-hint">
            Introduce una nueva contraseña para {{ resetTarget()?.username }}.
          </p>
          <ui-input-field
            label="Nueva contraseña"
            type="password"
            formControlName="newPassword"
            placeholder="Mínimo 8 caracteres, una mayúscula y un número"
            [error]="passwordError()"
          ></ui-input-field>
          @if (resetError()) {
            <ui-validation-message type="error" [message]="resetError()!" />
          }
          <div class="admin__modal-actions">
            <ui-button
              label="Cancelar"
              variant="ghost"
              (clicked)="closeReset()"
            ></ui-button>
            <ui-button
              label="Restablecer"
              [disabled]="resetForm.invalid || resetting()"
              [loading]="resetting()"
            ></ui-button>
          </div>
        </form>
      </ui-modal>
    </section>
  `,
  styles: [
    `
      .admin {
        width: min(100% - var(--ui-layout-gutter) * 2, 1024px);
        margin: var(--ui-spacing-lg) auto;
        padding: var(--ui-spacing-md);
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
      }

      .admin__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xl);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
      }

      .admin__loading,
      .admin__empty {
        margin: 0;
        font-family: var(--ui-font-family);
        color: var(--ui-color-on-surface-variant);
      }

      .admin__users {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-sm);
      }

      .admin__user {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--ui-spacing-md);
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline-variant);
        border-radius: var(--ui-radius-md);
      }

      .admin__user-info {
        display: flex;
        flex-direction: column;
        gap: 2px;
      }

      .admin__user-name {
        font-family: var(--ui-font-family);
        font-weight: var(--ui-font-weight-medium);
        color: var(--ui-color-on-surface);
      }

      .admin__user-role {
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface-variant);
      }

      .admin__reset-hint {
        margin: 0 0 var(--ui-spacing-sm);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface-variant);
      }

      .admin__modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: var(--ui-spacing-sm);
        margin-top: var(--ui-spacing-md);
      }

      @media (max-width: 640px) {
        .admin__users {
          overflow-x: auto;
        }

        .admin__user {
          min-width: 480px;
        }
      }
    `,
  ],
})
export class AdminComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly apiService = inject(ApiService);

  readonly users = signal<UserProfile[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly resetTarget = signal<UserProfile | null>(null);
  readonly resetError = signal<string | null>(null);
  readonly resetting = signal(false);

  readonly resetForm = this.fb.group({
    newPassword: ['', [Validators.required, passwordComplexity]],
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.apiService.getAdminUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set('Ha habido un error, inténtelo de nuevo');
      },
    });
  }

  passwordError(): string {
    const control = this.resetForm.controls['newPassword'];
    if (!(control.touched && control.invalid)) {
      return '';
    }
    const errors = control.errors ?? {};
    if (errors['required']) {
      return 'La contraseña es obligatoria';
    }
    if (errors['passwordLength']) {
      return 'Mínimo 8 caracteres';
    }
    if (errors['passwordUppercase']) {
      return 'Debe incluir una letra mayúscula';
    }
    if (errors['passwordDigit']) {
      return 'Debe incluir un número';
    }
    return '';
  }

  openReset(user: UserProfile): void {
    this.resetTarget.set(user);
    this.resetError.set(null);
    this.resetForm.reset();
  }

  closeReset(): void {
    if (this.resetting()) return;
    this.resetTarget.set(null);
    this.resetError.set(null);
  }

  submitReset(): void {
    const target = this.resetTarget();
    if (!target || this.resetting() || this.resetForm.invalid) return;

    this.resetting.set(true);
    this.resetError.set(null);
    const newPassword = this.resetForm.value.newPassword?.trim() ?? '';

    this.apiService.resetUserPassword(target.id, newPassword).subscribe({
      next: () => {
        this.resetting.set(false);
        this.closeReset();
        this.successMessage.set('Contraseña actualizada');
      },
      error: (error: unknown) => {
        this.resetting.set(false);
        this.resetError.set(this.extractError(error));
      },
    });
  }

  private extractError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiError | undefined;
      if (body?.error) {
        return body.error;
      }
    }
    return 'Ha habido un error, inténtelo de nuevo';
  }
}