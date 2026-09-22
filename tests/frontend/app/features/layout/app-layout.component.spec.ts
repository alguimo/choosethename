import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AppLayoutComponent } from '@app/features/layout/app-layout.component';
import { AuthService } from '@app/services/auth.service';
import { DashboardService } from '@app/services/dashboard.service';
import { LocalStorageService } from '@app/services/local-storage.service';

describe('AppLayoutComponent', () => {
  let component: AppLayoutComponent;
  let fixture: ComponentFixture<AppLayoutComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let dashboardSpy: jasmine.SpyObj<DashboardService>;
  let localStorageSpy: jasmine.SpyObj<LocalStorageService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['logout']);
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
});
