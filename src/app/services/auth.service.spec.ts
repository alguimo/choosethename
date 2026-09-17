import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
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
});
