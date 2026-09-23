import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiInputFieldComponent } from '../../ui-kit/atoms/input-field/input-field.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, UiButtonComponent, UiInputFieldComponent, UiValidationMessageComponent],
  template: `
    <div class="login-container">
      <h2>Iniciar Sesión</h2>

      @if (registeredMessage) {
        <ui-validation-message
          type="info"
          message="Cuenta creada correctamente. Inicia sesión."
        />
      }

      <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
        <ui-input-field
          label="Usuario"
          formControlName="username"
          placeholder="Tu nombre de usuario">
        </ui-input-field>
        
        <ui-input-field
          label="Contraseña"
          type="password"
          formControlName="password"
          placeholder="Tu contraseña">
        </ui-input-field>

        @if (error()) {
          <ui-validation-message type="error" [message]="error()!" />
        }

        <ui-button
          type="submit"
          [label]="loading() ? 'Entrando...' : 'Entrar'"
          [disabled]="loginForm.invalid || loading()"
        ></ui-button>

        <ui-button
          variant="secondary"
          label="Crear cuenta"
          (clicked)="goToRegister()"
        ></ui-button>
      </form>
    </div>
  `,
  styles: [`
    .login-container { max-width: 400px; margin: 2rem auto; padding: 1rem; }
    form { display: flex; flex-direction: column; gap: 1rem; }
  `]
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private apiService = inject(ApiService);
  private router = inject(Router);

  loginForm: FormGroup = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  loading = signal(false);
  error = signal<string | null>(null);
  registeredMessage = false;

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    this.registeredMessage = navigation?.extras?.state?.['registered'] === true;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    this.apiService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.authService.setToken(res.accessToken);
        this.authService.loadProfile().subscribe();
        this.router.navigate(['/']);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Usuario o contraseña incorrectos');
      }
    });
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }
}
