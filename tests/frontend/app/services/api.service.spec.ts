import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiService } from '@app/services/api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function expectReq(method: string, url: string) {
    const req = httpMock.expectOne(url);
    expect(req.request.method).toBe(method);
    return req;
  }

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST to /auth/login', () => {
    const credentials = { username: 'alvaro', password: 'secret' };
    service.login(credentials).subscribe();
    const req = expectReq('POST', '/api/v1/auth/login');
    expect(req.request.body).toEqual(credentials);
    req.flush({ accessToken: 'fake-token', tokenType: 'Bearer' });
  });

  it('should POST to /lists', () => {
    const body = { name: 'La pandilla' };
    service.createList(body).subscribe();
    const req = expectReq('POST', '/api/v1/lists');
    expect(req.request.body).toEqual(body);
    req.flush({ id: '1' });
  });

  it('should POST to /lists/join', () => {
    const body = { code: 'ABC123' };
    service.joinList(body).subscribe();
    const req = expectReq('POST', '/api/v1/lists/join');
    expect(req.request.body).toEqual(body);
    req.flush({ id: '1' });
  });

  it('should GET /lists/active', () => {
    service.getActiveList().subscribe();
    expectReq('GET', '/api/v1/lists/active').flush({ id: '1' });
  });

  it('should PATCH /lists/{id}/close-invitations', () => {
    service.closeInvitations('1').subscribe();
    expectReq('PATCH', '/api/v1/lists/1/close-invitations').flush({});
  });

  it('should POST to /lists/{id}/names', () => {
    const body = { names: ['Pablo'] };
    service.addNames('1', body).subscribe();
    const req = expectReq('POST', '/api/v1/lists/1/names');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('should POST to /lists/{id}/finish-addition', () => {
    service.finishAddition('1').subscribe();
    expectReq('POST', '/api/v1/lists/1/finish-addition').flush({});
  });

  it('should GET /lists/{id}/selection', () => {
    service.getSelection('1').subscribe();
    expectReq('GET', '/api/v1/lists/1/selection').flush({});
  });

  it('should POST to /lists/{id}/selection/adopt', () => {
    service.adoptFadedName('1', 'Fondor').subscribe();
    expectReq('POST', '/api/v1/lists/1/selection/adopt').flush({});
  });

  it('should POST to /lists/{id}/complete-selection', () => {
    service.completeSelection('1').subscribe();
    expectReq('POST', '/api/v1/lists/1/complete-selection').flush({});
  });

  it('should POST to /lists/{id}/vote', () => {
    const body = { roundNumber: 1, rankings: ['a', 'b'] };
    service.submitVote('1', body).subscribe();
    const req = expectReq('POST', '/api/v1/lists/1/vote');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('should GET /lists/{id}/results', () => {
    service.getResults('1').subscribe();
    expectReq('GET', '/api/v1/lists/1/results').flush({ results: [] });
  });
});