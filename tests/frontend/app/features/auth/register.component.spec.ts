import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent } from '@app/features/auth/register.component';
import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Router } from '@angular/router';
import { ApiService } from '@app/services/api.service';
import { of, throwError } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiServiceSpy = jasmine.createSpyObj('ApiService', ['register']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        provideAnimationsAsync(),
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the username and password fields and the action buttons', () => {
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Usuario');
    expect(text).toContain('Contraseña');
    expect(text).toContain('Registrarse');
    expect(text).toContain('Volver');
  });

  it('TS-31: should register with valid data, redirect to login with the success state and store no token', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'Secret12' });
    apiServiceSpy.register.and.returnValue(
      of({ id: 1, username: 'alvaro', role: 'PARTICIPANT' }),
    );

    component.onSubmit();

    expect(apiServiceSpy.register).toHaveBeenCalledWith({
      username: 'alvaro',
      password: 'Secret12',
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], {
      state: { registered: true },
    });
    expect(component.error()).toBeNull();
  });

  it('TS-32: should show the 409 error, retain the form values and not navigate', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'Secret12' });
    apiServiceSpy.register.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: {} })),
    );

    component.onSubmit();
    fixture.detectChanges();

    expect(component.error()).toBe('El usuario ya existe');
    expect(fixture.nativeElement.textContent).toContain('El usuario ya existe');
    expect(component.registerForm.value).toEqual({ username: 'alvaro', password: 'Secret12' });
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should show a verification error on 400 and retain the form values', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'Secret12' });
    apiServiceSpy.register.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: {} })),
    );

    component.onSubmit();
    fixture.detectChanges();

    expect(component.error()).toBe('Verifica los datos introducidos');
    expect(component.registerForm.value).toEqual({ username: 'alvaro', password: 'Secret12' });
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('TS-33: should show the minimum length message for a short password and send no request', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'abc' });
    component.registerForm.controls['password'].markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Mínimo 8 caracteres');

    component.onSubmit();
    expect(apiServiceSpy.register).not.toHaveBeenCalled();
  });

  it('should show the uppercase message when the password has no uppercase letter', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'secrets1' });
    component.registerForm.controls['password'].markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Debe incluir una letra mayúscula');
  });

  it('should show the digit message when the password has no digit', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'Secretoxx' });
    component.registerForm.controls['password'].markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Debe incluir un número');
  });

  it('TS-34: should show the required message for blank inputs and send no request', () => {
    component.registerForm.setValue({ username: '   ', password: '' });
    component.registerForm.controls['username'].markAsTouched();
    component.registerForm.controls['password'].markAsTouched();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('El usuario es obligatorio');

    component.onSubmit();
    expect(apiServiceSpy.register).not.toHaveBeenCalled();
  });

  it('TS-35: should show a generic error on network failure, retain the form and not navigate', () => {
    component.registerForm.setValue({ username: 'alvaro', password: 'Secret12' });
    apiServiceSpy.register.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 0, error: null })),
    );

    component.onSubmit();
    fixture.detectChanges();

    expect(component.error()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(fixture.nativeElement.textContent).toContain('Ha habido un error, inténtelo de nuevo');
    expect(component.registerForm.value).toEqual({ username: 'alvaro', password: 'Secret12' });
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});