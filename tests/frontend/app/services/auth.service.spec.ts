import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '@app/services/auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should set and get token', () => {
    service.setToken('test-token');
    expect(service.getToken()).toBe('test-token');
    expect(localStorage.getItem('auth_token')).toBe('test-token');
  });

  it('should clear token on logout', () => {
    service.setToken('test-token');
    service.logout();
    expect(service.getToken()).toBeNull();
    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('TS-45: should fetch the profile via /auth/me and expose it as ADMIN when the role is ADMIN', () => {
    service.setToken('test-token');

    service.loadProfile().subscribe((profile) => {
      expect(profile?.username).toBe('alvaro');
    });

    const request = httpTesting.expectOne('/api/v1/auth/me');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 1, username: 'alvaro', role: 'ADMIN' });

    expect(service.getProfile()).toEqual({ id: 1, username: 'alvaro', role: 'ADMIN' });
    expect(service.isAdmin()).toBeTrue();
  });

  it('should expose a non-admin profile and clear it on logout', () => {
    service.setToken('test-token');
    service.loadProfile().subscribe();

    httpTesting.expectOne('/api/v1/auth/me').flush({
      id: 2,
      username: 'ana',
      role: 'PARTICIPANT',
    });

    expect(service.isAdmin()).toBeFalse();

    service.logout();
    expect(service.getProfile()).toBeNull();
    expect(service.isAdmin()).toBeFalse();
  });

  it('should keep the profile null when fetching the profile fails', () => {
    service.setToken('test-token');
    service.loadProfile().subscribe();

    httpTesting.expectOne('/api/v1/auth/me').flush(
      {},
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(service.getProfile()).toBeNull();
    expect(service.isAdmin()).toBeFalse();
  });
});