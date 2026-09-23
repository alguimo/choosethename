import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DashboardService, NOW_PROVIDER } from '@app/services/dashboard.service';
import { ApiService } from '@app/services/api.service';
import { ListResponse } from '@app/models/api.models';

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
  myStepCompleted: false,
};

const SECOND: ListResponse = { ...LIST, id: '2', name: 'Otra lista' };

describe('DashboardService', () => {
  let service: DashboardService;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let now = 0;

  beforeEach(() => {
    now = 0;
    apiSpy = jasmine.createSpyObj('ApiService', ['getMyLists']);
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

  it('should fetch the lists from the API on the first call', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST, SECOND]));

    let result: ListResponse[] | undefined;
    service.getMyLists().subscribe((lists) => (result = lists));

    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);
    expect(result).toEqual([LIST, SECOND]);
  });

  it('TS-40: should return an empty array when the user has no lists', () => {
    apiSpy.getMyLists.and.returnValue(of([]));

    let result: ListResponse[] | undefined;
    service.getMyLists().subscribe((lists) => (result = lists));

    expect(result).toEqual([]);
  });

  it('should reuse the cached lists when called again within the TTL', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));

    service.getMyLists().subscribe(() => {});

    let result: ListResponse[] | undefined;
    service.getMyLists().subscribe((lists) => (result = lists));

    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);
    expect(result).toEqual([LIST]);
  });

  it('TS-28: should re-fetch from the API once the 5-minute TTL elapses', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));

    now = 1_000_000;
    service.getMyLists().subscribe(() => {});
    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);

    now = 1_000_000 + TTL_MS - 1;
    service.getMyLists().subscribe(() => {});
    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);

    now = 1_000_000 + TTL_MS;
    service.getMyLists().subscribe(() => {});
    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(2);
  });

  it('should propagate errors', () => {
    apiSpy.getMyLists.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: {} })),
    );

    let received: unknown;
    service.getMyLists().subscribe({ error: (error: unknown) => (received = error) });

    expect(received).toBeInstanceOf(HttpErrorResponse);
  });

  it('should invalidate the cache forcing a refetch', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));

    service.getMyLists().subscribe(() => {});
    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);

    service.invalidate();
    service.getMyLists().subscribe(() => {});

    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(2);
  });
});
