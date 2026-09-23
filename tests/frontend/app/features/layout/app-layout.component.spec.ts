import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AppLayoutComponent } from '@app/features/layout/app-layout.component';
import { AuthService } from '@app/services/auth.service';
import { DashboardService } from '@app/services/dashboard.service';
import { LocalStorageService } from '@app/services/local-storage.service';
import { UserProfile } from '@app/models/api.models';

describe('AppLayoutComponent', () => {
  let component: AppLayoutComponent;
  let fixture: ComponentFixture<AppLayoutComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let dashboardSpy: jasmine.SpyObj<DashboardService>;
  let localStorageSpy: jasmine.SpyObj<LocalStorageService>;
  let router: Router;
  let adminSignal: ReturnType<typeof signal<boolean>>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj(
      'AuthService',
      ['logout', 'getToken', 'loadProfile', 'getProfile'],
    );
    authSpy.getToken.and.returnValue(null);
    authSpy.getProfile.and.returnValue(null);
    adminSignal = signal(false);
    Object.defineProperty(authSpy, 'isAdmin', { value: adminSignal, configurable: true });
    dashboardSpy = jasmine.createSpyObj('DashboardService', ['invalidate']);
    localStorageSpy = jasmine.createSpyObj('LocalStorageService', ['clearAllListCaches']);

    await TestBed.configureTestingModule({
      imports: [AppLayoutComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: DashboardService, useValue: dashboardSpy },
        { provide: LocalStorageService, useValue: localStorageSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(AppLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('TS-16: should render the app bar with the title and a router outlet', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('ui-app-bar')).toBeTruthy();
    expect(element.querySelector('router-outlet')).toBeTruthy();
    expect(element.textContent).toContain('Elegir el Nombre');
  });

  it('TS-17: should clear the session and caches and return to login on logout', () => {
    component.logout();

    expect(authSpy.logout).toHaveBeenCalled();
    expect(dashboardSpy.invalidate).toHaveBeenCalled();
    expect(localStorageSpy.clearAllListCaches).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('FR-63: should navigate home when the title is clicked', () => {
    component.goHome();

    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('TS-45: should fetch the profile on init when a token is already stored', () => {
    authSpy.getToken.and.returnValue('stored-token');
    authSpy.loadProfile.and.returnValue(of<UserProfile | null>({ id: 1, username: 'alvaro', role: 'ADMIN' }));

    component.ngOnInit();

    expect(authSpy.loadProfile).toHaveBeenCalled();
  });

  it('should not fetch the profile when there is no stored token', () => {
    component.ngOnInit();

    expect(authSpy.loadProfile).not.toHaveBeenCalled();
  });

  it('TS-45: should render the admin action only for an ADMIN profile', () => {
    authSpy.getProfile.and.returnValue({ id: 1, username: 'alvaro', role: 'ADMIN' });
    adminSignal.set(true);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Admin');

    component.goAdmin();
    expect(router.navigate).toHaveBeenCalledWith(['/admin']);
  });
});
