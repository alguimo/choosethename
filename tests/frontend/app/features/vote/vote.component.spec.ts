import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { VoteComponent } from '@app/features/vote/vote.component';
import { ApiService } from '@app/services/api.service';
import { AuthService } from '@app/services/auth.service';
import { DashboardService } from '@app/services/dashboard.service';
import { LocalStorageService } from '@app/services/local-storage.service';
import { ListResponse } from '@app/models/api.models';
import { UiDraggableRankingListComponent } from '@app/ui-kit/organisms/draggable-ranking-list/draggable-ranking-list.component';

const LIST: ListResponse = {
  id: '1',
  name: 'La pandilla',
  invitationCode: 'ABC123',
  codeExpiresAt: '2026-09-19T10:00:00Z',
  phase: 'VOTING',
  invitationsOpen: true,
  ownerUsername: 'alvaro',
  currentRound: 1,
  totalRounds: 3,
  currentPool: ['A', 'B', 'C'],
  members: [],
};

const apiError = (status: number) =>
  throwError(() => new HttpErrorResponse({ status, error: {} }));

describe('VoteComponent', () => {
  let component: VoteComponent;
  let fixture: ComponentFixture<VoteComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let dashboardSpy: jasmine.SpyObj<DashboardService>;
  let localStorageSpy: jasmine.SpyObj<LocalStorageService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getListById', 'submitVote', 'login']);
    apiSpy.getListById.and.returnValue(of(LIST));
    apiSpy.submitVote.and.returnValue(of(LIST));
    apiSpy.login.and.returnValue(of({ accessToken: 'new-token', tokenType: 'Bearer' }));
    authSpy = jasmine.createSpyObj('AuthService', ['getToken', 'setToken', 'logout']);
    authSpy.getToken.and.returnValue(null);
    dashboardSpy = jasmine.createSpyObj('DashboardService', ['invalidate']);
    localStorageSpy = jasmine.createSpyObj('LocalStorageService', [
      'getItem',
      'setItem',
      'removeItem',
      'clearListCache',
      'isAvailable',
    ]);
    localStorageSpy.getItem.and.returnValue(null);
    localStorageSpy.isAvailable.and.returnValue(true);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [VoteComponent],
      providers: [
        { provide: ApiService, useValue: apiSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: DashboardService, useValue: dashboardSpy },
        { provide: LocalStorageService, useValue: localStorageSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (): string | null => '1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VoteComponent);
    component = fixture.componentInstance;
  });

  const renderedText = (): string =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  const rankingList = () =>
    fixture.debugElement.query(By.directive(UiDraggableRankingListComponent))
      .componentInstance as UiDraggableRankingListComponent;

  const rankItems = () =>
    (fixture.nativeElement as HTMLElement).querySelectorAll('.ui-ranking-list__item').length;

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('TS-19: should load the pool and display the round indicator', () => {
    fixture.detectChanges();

    expect(apiSpy.getListById).toHaveBeenCalled();
    expect(component.roundNumber()).toBe(1);
    expect(component.totalRounds()).toBe(3);
    expect(component.pool()).toEqual(['A', 'B', 'C']);
    expect(renderedText()).toContain('Ronda 1 de 3');
    expect(rankItems()).toBe(3);
  });

  it('FR-47: should show an error when the voting view cannot be loaded', () => {
    apiSpy.getListById.and.returnValue(apiError(500));

    fixture.detectChanges();

    expect(component.submitError()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(renderedText()).toContain('Ha habido un error, inténtelo de nuevo');
  });

  it('FR-33: should restore a saved ranking for the current round', () => {
    localStorageSpy.getItem.and.returnValue(['B', 'A', 'C']);

    fixture.detectChanges();

    expect(component.ranking()).toEqual(['B', 'A', 'C']);
  });

  it('TS-29: should discard a stale ranking with names no longer in the pool', () => {
    localStorageSpy.getItem.and.returnValue(['X', 'Y', 'B']);

    fixture.detectChanges();

    expect(component.ranking()).toEqual(['A', 'B', 'C']);
  });

  it('TS-20: should persist the new order when the ranking is reordered', () => {
    fixture.detectChanges();

    rankingList().rankingsChanged.emit(['B', 'A', 'C']);
    fixture.detectChanges();

    expect(component.ranking()).toEqual(['B', 'A', 'C']);
    expect(localStorageSpy.setItem).toHaveBeenCalledWith(
      '1',
      'vote_round_1',
      ['B', 'A', 'C'],
    );
  });

  it('TS-21: should submit the vote, clear localStorage, and navigate home', () => {
    fixture.detectChanges();

    component.submitVote();

    expect(apiSpy.submitVote).toHaveBeenCalledWith('1', {
      roundNumber: 1,
      rankings: ['A', 'B', 'C'],
    });
    expect(localStorageSpy.clearListCache).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    expect(component.submitting()).toBeFalse();
  });

  it('FR-17: should invalidate the dashboard cache after a successful vote', () => {
    fixture.detectChanges();

    component.submitVote();

    expect(dashboardSpy.invalidate).toHaveBeenCalledTimes(1);
  });

  it('TS-22: should retain the ranking and show an error on 409', () => {
    apiSpy.submitVote.and.returnValue(apiError(409));
    fixture.detectChanges();

    component.submitVote();
    fixture.detectChanges();

    expect(component.submitError()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(localStorageSpy.clearListCache).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(renderedText()).toContain('Ha habido un error, inténtelo de nuevo');
    expect(dashboardSpy.invalidate).not.toHaveBeenCalled();
  });

  it('TS-23: should mark the ranking with a red border on 400/422', () => {
    apiSpy.submitVote.and.returnValue(apiError(422));
    fixture.detectChanges();

    component.submitVote();
    fixture.detectChanges();

    expect(component.invalid()).toBeTrue();
    expect(component.submitError()).toBe('Voto no válido. Revisa el orden de los nombres');
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.vote__ranking--invalid'),
    ).toBeTruthy();
    expect(renderedText()).toContain('Voto no válido. Revisa el orden de los nombres');
  });

  it('TS-26: should prompt for re-authentication and retry the vote on 401', () => {
    authSpy.setToken.and.callFake((token: string) => {
      authSpy.getToken.and.returnValue(token);
    });
    apiSpy.submitVote.and.callFake(() =>
      authSpy.getToken() === 'new-token' ? of(LIST) : apiError(401),
    );
    fixture.detectChanges();

    component.submitVote();
    fixture.detectChanges();

    expect(component.showReAuth()).toBeTrue();
    expect(renderedText()).toContain('Tu sesión ha caducado');
    expect(localStorageSpy.clearListCache).not.toHaveBeenCalled();

    component.reAuthForm.setValue({ username: 'alvaro', password: 'pass' });
    component.onReAuthSubmit();
    fixture.detectChanges();

    expect(authSpy.setToken).toHaveBeenCalledWith('new-token');
    expect(apiSpy.submitVote).toHaveBeenCalledTimes(2);
    expect(localStorageSpy.clearListCache).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('TS-27: should warn and keep the ranking usable when localStorage is unavailable', () => {
    localStorageSpy.isAvailable.and.returnValue(false);
    fixture.detectChanges();

    expect(renderedText()).toContain(
      'Tu progreso no se guardará localmente. No cierres la página.',
    );
    expect(rankItems()).toBe(3);

    component.onRankingsChanged(['C', 'A', 'B']);
    fixture.detectChanges();

    expect(component.ranking()).toEqual(['C', 'A', 'B']);
  });
});