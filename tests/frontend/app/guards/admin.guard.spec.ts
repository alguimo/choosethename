import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '@app/services/auth.service';
import { UserProfile } from '@app/models/api.models';
import { adminGuard } from '@app/guards/admin.guard';

describe('adminGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', [
      'getToken',
      'getProfile',
      'loadProfile',
    ]);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });
  });

  it('TS-51: should redirect to /login when there is no token', () => {
    authService.getToken.and.returnValue(null);

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect((result as UrlTree).toString()).toBe('/login');
  });

  it('TS-46: should allow an ADMIN profile to navigate', () => {
    authService.getToken.and.returnValue('token');
    authService.getProfile.and.returnValue({ id: 1, username: 'alvaro', role: 'ADMIN' });

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect(result).toBe(true);
    expect(authService.loadProfile).not.toHaveBeenCalled();
  });

  it('TS-50: should redirect to / when the profile is a PARTICIPANT', () => {
    authService.getToken.and.returnValue('token');
    const participant: UserProfile = { id: 2, username: 'ana', role: 'PARTICIPANT' };
    authService.loadProfile.and.returnValue(of(participant));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;

    result.subscribe((redirect) => {
      expect((redirect as UrlTree).toString()).toBe('/');
    });
  });

  it('should redirect to / when the profile cannot be loaded', () => {
    authService.getToken.and.returnValue('token');
    authService.loadProfile.and.returnValue(of(null));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;

    result.subscribe((redirect) => {
      expect((redirect as UrlTree).toString()).toBe('/');
    });
  });
});