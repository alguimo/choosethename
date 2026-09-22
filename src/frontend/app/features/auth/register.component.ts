import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { passwordComplexity, nonWhitespaceOnly } from '../../auth-validators';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiInputFieldComponent } from '../../ui-kit/atoms/input-field/input-field.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, UiButtonComponent, UiInputFieldComponent, UiValidationMessageComponent],
  template: `
    <div class="register-container">
      <h2>Crear Cuenta</h2>
      <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
        <ui-input-field
          label="Usuario"
          formControlName="username"
          placeholder="Elige un nombre de usuario"
          [error]="usernameError()">
        </ui-input-field>

        <ui-input-field
          label="Contraseña"
          type="password"
          formControlName="password"
          placeholder="Mínimo 8 caracteres, una mayúscula y un número"
          [error]="passwordError()">
        </ui-input-field>

        @if (error()) {
          <ui-validation-message type="error" [message]="error()!" />
        }

        <ui-button
          type="submit"
          [label]="loading() ? 'Registrando...' : 'Registrarse'"
          [disabled]="registerForm.invalid || loading()"
        ></ui-button>

        <ui-button
          variant="ghost"
          label="Volver"
          (clicked)="goToLogin()"
        ></ui-button>
      </form>
    </div>
  `,
  styles: [`
    .register-container { max-width: 400px; margin: 2rem auto; padding: 1rem; }
    form { display: flex; flex-direction: column; gap: 1rem; }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private apiService = inject(ApiService);
  private router = inject(Router);

  registerForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, nonWhitespaceOnly]],
    password: ['', [Validators.required, passwordComplexity]],
  });

  loading = signal(false);
  error = signal<string | null>(null);

  usernameError(): string {
    const control = this.registerForm.controls['username'];
    if (!(control.touched && control.invalid)) {
      return '';
    }
    return 'El usuario es obligatorio';
  }

  passwordError(): string {
    const control = this.registerForm.controls['password'];
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

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.apiService.register(this.registerForm.value).subscribe({
      next: () => {
        this.router.navigate(['/login'], { state: { registered: true } });
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 409) {
          this.error.set('El usuario ya existe');
        } else if (err.status >= 400 && err.status < 500) {
          this.error.set('Verifica los datos introducidos');
        } else {
          this.error.set('Ha habido un error, inténtelo de nuevo');
        }
      },
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}