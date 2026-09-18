import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from '@app/features/auth/login.component';
import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Router } from '@angular/router';
import { AuthService } from '@app/services/auth.service';
import { ApiService } from '@app/services/api.service';
import { of, throwError } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['setToken']);
    apiServiceSpy = jasmine.createSpyObj('ApiService', ['login']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        provideAnimationsAsync(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the submit button label', () => {
    const button = fixture.nativeElement.querySelector(
      'ui-button button',
    ) as HTMLButtonElement;

    expect(button.textContent?.trim()).toBe('Entrar');
  });

  it('should call login on submit', () => {
    component.loginForm.setValue({ username: 'test', password: 'password' });
    apiServiceSpy.login.and.returnValue(of({ accessToken: 'fake', tokenType: 'Bearer' }));

    component.onSubmit();
    
    expect(apiServiceSpy.login).toHaveBeenCalled();
    expect(authServiceSpy.setToken).toHaveBeenCalledWith('fake');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('TS-2: should show an inline error and preserve the form when credentials are invalid', () => {
    component.loginForm.setValue({ username: 'test', password: 'wrong' });
    apiServiceSpy.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401, error: {} })),
    );

    component.onSubmit();
    fixture.detectChanges();

    expect(component.error()).toBe('Usuario o contraseña incorrectos');
    expect(fixture.nativeElement.textContent).toContain('Usuario o contraseña incorrectos');
    expect(component.loginForm.value).toEqual({ username: 'test', password: 'wrong' });
    expect(authServiceSpy.setToken).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
