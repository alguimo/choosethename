import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should attach the Authorization Bearer header when a token exists', () => {
    authService.setToken('fake-token');

    httpClient.get('/api/v1/lists/active').subscribe();

    const req = httpTesting.expectOne('/api/v1/lists/active');
    expect(req.request.headers.get('Authorization')).toBe('Bearer fake-token');
    req.flush({});
  });

  it('should not attach an Authorization header when no token exists', () => {
    authService.logout();

    httpClient.get('/api/v1/lists/active').subscribe();

    const req = httpTesting.expectOne('/api/v1/lists/active');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});