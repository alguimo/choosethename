import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DashboardService, NOW_PROVIDER } from './dashboard.service';
import { ApiService } from './api.service';
import { ListResponse } from '../models/api.models';

const TTL_MS = 5 * 60 * 1000;

const LIST: ListResponse = {
  id: '1',
  name: 'La pandilla',
  invitationCode: 'ABC123',
  codeExpiresAt: '2026-09-19T10:00:00Z',
  phase: 'ADDITION',
  invitationsOpen: true,
  ownerUsername: 'alvaro',
  currentRound: 1,
  totalRounds: 1,
  currentPool: [],
  members: [{ id: 'u1', username: 'alvaro' }],
};

describe('DashboardService', () => {
  let service: DashboardService;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let now = 0;

  beforeEach(() => {
    now = 0;
    apiSpy = jasmine.createSpyObj('ApiService', ['getActiveList']);
    TestBed.configureTestingModule({
      providers: [
        DashboardService,
        { provide: ApiService, useValue: apiSpy },
        { provide: NOW_PROVIDER, useValue: () => now },
      ],
    });
    service = TestBed.inject(DashboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch the active list from the API on the first call', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));

    let result: ListResponse | null | undefined;
    service.getActiveList().subscribe((list) => (result = list));

    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);
    expect(result).toBe(LIST);
  });

  it('should reuse the cached list when called again within the TTL', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));

    service.getActiveList().subscribe(() => {});

    let result: ListResponse | null | undefined;
    service.getActiveList().subscribe((list) => (result = list));

    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);
    expect(result).toBe(LIST);
  });

  it('TS-28: should re-fetch from the API once the 5-minute TTL elapses', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));

    now = 1_000_000;
    service.getActiveList().subscribe(() => {});
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);

    now = 1_000_000 + TTL_MS - 1;
    service.getActiveList().subscribe(() => {});
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);

    now = 1_000_000 + TTL_MS;
    service.getActiveList().subscribe(() => {});
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(2);
  });

  it('should expose null and cache it when the API returns 404', () => {
    apiSpy.getActiveList.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: {} })),
    );

    let result: ListResponse | null | undefined;
    service.getActiveList().subscribe((list) => (result = list));
    expect(result).toBeNull();

    service.getActiveList().subscribe((list) => (result = list));
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);
    expect(result).toBeNull();
  });

  it('should propagate non-404 errors', () => {
    apiSpy.getActiveList.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: {} })),
    );

    let received: unknown;
    service.getActiveList().subscribe({ error: (error: unknown) => (received = error) });

    expect(received).toBeInstanceOf(HttpErrorResponse);
  });

  it('should invalidate the cache forcing a refetch', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));

    service.getActiveList().subscribe(() => {});
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);

    service.invalidate();
    service.getActiveList().subscribe(() => {});

    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(2);
  });
});