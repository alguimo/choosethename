import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SuggestionComponent } from '@app/features/suggestion/suggestion.component';
import { ApiService } from '@app/services/api.service';
import { AuthService } from '@app/services/auth.service';
import { DashboardService } from '@app/services/dashboard.service';
import { LocalStorageService } from '@app/services/local-storage.service';
import { ListResponse } from '@app/models/api.models';

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
  members: [],
};

const apiError = (status: number) =>
  throwError(() => new HttpErrorResponse({ status, error: {} }));

describe('SuggestionComponent', () => {
  let component: SuggestionComponent;
  let fixture: ComponentFixture<SuggestionComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let dashboardSpy: jasmine.SpyObj<DashboardService>;
  let localStorageSpy: jasmine.SpyObj<LocalStorageService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', [
      'getActiveList',
      'addNames',
      'finishAddition',
      'login',
    ]);
    apiSpy.getActiveList.and.returnValue(of(LIST));
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
      imports: [SuggestionComponent],
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

    fixture = TestBed.createComponent(SuggestionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const renderedText = (): string =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('FR-18: should restore saved suggestions from localStorage on entry', () => {
    localStorageSpy.getItem.and.returnValue(['Pablo', 'Lucía']);

    const freshFixture = TestBed.createComponent(SuggestionComponent);
    const freshComponent = freshFixture.componentInstance;
    freshFixture.detectChanges();

    expect(freshComponent.names()).toEqual(['Pablo', 'Lucía']);
    expect((freshFixture.nativeElement as HTMLElement).textContent).toContain('Pablo');
  });

  it('TS-11: should add a valid name, persist it, and display it', () => {
    component.addName('Morena');
    fixture.detectChanges();

    expect(component.names()).toEqual(['Morena']);
    expect(localStorageSpy.setItem).toHaveBeenCalledWith('1', 'suggestions', ['Morena']);
    expect(renderedText()).toContain('Morena');
  });

  it('TS-9: should reject input with invalid characters and show a validation message', () => {
    component.addName('Morena 123');
    fixture.detectChanges();

    expect(component.names()).toEqual([]);
    expect(localStorageSpy.setItem).not.toHaveBeenCalled();
    expect(renderedText()).toContain('Solo se permiten letras, espacios y guiones');
  });

  it('TS-9: should reject punctuation and diacritic-less special characters', () => {
    component.addName('Ana!');
    component.addName('6');
    fixture.detectChanges();

    expect(component.names()).toEqual([]);
  });

  it('TS-10: should reject a duplicate name (case/accent-insensitive) with a warning', () => {
    component.addName('Pablo');
    component.addName('PABLO');
    component.addName('Páblo');
    fixture.detectChanges();

    expect(component.names()).toEqual(['Pablo']);
    expect(localStorageSpy.setItem).toHaveBeenCalledTimes(1);
    expect(renderedText()).toContain('Este nombre ya está en la lista');
  });

  it('TS-12: should remove a suggestion and persist the change', () => {
    component.addName('Pablo');
    component.addName('Lucía');
    component.removeName(0);
    fixture.detectChanges();

    expect(component.names()).toEqual(['Lucía']);
    expect(localStorageSpy.setItem).toHaveBeenCalledWith('1', 'suggestions', ['Lucía']);
    expect(renderedText()).not.toContain('Pablo');
  });

  it('TS-13: should block finishing without suggestions and show a message', () => {
    component.completeAddition();
    fixture.detectChanges();

    expect(component.submitting()).toBeFalse();
    expect(component.submitError()).toBe('Debes añadir al menos un nombre');
    expect(apiSpy.addNames).not.toHaveBeenCalled();
    expect(apiSpy.finishAddition).not.toHaveBeenCalled();
    expect(renderedText()).toContain('Debes añadir al menos un nombre');
  });

  it('TS-14: should send names, finish the phase, clear localStorage, and navigate home', () => {
    component.addName('Pablo');
    localStorageSpy.setItem.calls.reset();
    apiSpy.addNames.and.returnValue(of(undefined));
    apiSpy.finishAddition.and.returnValue(of({} as ListResponse));

    component.completeAddition();

    expect(apiSpy.addNames).toHaveBeenCalledWith('1', { names: ['Pablo'] });
    expect(apiSpy.finishAddition).toHaveBeenCalledWith('1');
    expect(localStorageSpy.clearListCache).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    expect(component.submitting()).toBeFalse();
  });

  it('FR-17: should invalidate the dashboard cache after completing the phase', () => {
    component.addName('Pablo');
    apiSpy.addNames.and.returnValue(of(undefined));
    apiSpy.finishAddition.and.returnValue(of({} as ListResponse));

    component.completeAddition();

    expect(dashboardSpy.invalidate).toHaveBeenCalledTimes(1);
  });

  it('TS-15: should show an error and retain localStorage when submitting names fails', () => {
    component.addName('Pablo');
    apiSpy.addNames.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: {} })),
    );

    component.completeAddition();
    fixture.detectChanges();

    expect(component.submitError()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(component.names()).toEqual(['Pablo']);
    expect(localStorageSpy.clearListCache).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(renderedText()).toContain('Ha habido un error, inténtelo de nuevo');
    expect(dashboardSpy.invalidate).not.toHaveBeenCalled();
  });

  it('FR-26: should show an error and retain localStorage when finishing the addition fails', () => {
    component.addName('Pablo');
    apiSpy.addNames.and.returnValue(of(undefined));
    apiSpy.finishAddition.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: {} })),
    );

    component.completeAddition();

    expect(component.submitError()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(localStorageSpy.clearListCache).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(component.submitting()).toBeFalse();
  });

  it('TS-27: should warn and keep allowing suggestions when localStorage is unavailable', () => {
    localStorageSpy.isAvailable.and.returnValue(false);
    fixture.detectChanges();

    expect(renderedText()).toContain(
      'Tu progreso no se guardará localmente. No cierres la página.',
    );

    component.addName('Morena');
    fixture.detectChanges();

    expect(component.names()).toEqual(['Morena']);
    expect(renderedText()).toContain('Morena');
  });

  it('TS-26: should prompt for re-authentication and retry the completion on 401', () => {
    authSpy.setToken.and.callFake((token: string) => {
      authSpy.getToken.and.returnValue(token);
    });
    component.addName('Pablo');
    localStorageSpy.setItem.calls.reset();
    apiSpy.addNames.and.callFake(() =>
      authSpy.getToken() === 'new-token' ? of(undefined) : apiError(401),
    );
    apiSpy.finishAddition.and.returnValue(of({} as ListResponse));

    component.completeAddition();
    fixture.detectChanges();

    expect(component.showReAuth()).toBeTrue();
    expect(renderedText()).toContain('Tu sesión ha caducado');
    expect(apiSpy.finishAddition).not.toHaveBeenCalled();
    expect(localStorageSpy.clearListCache).not.toHaveBeenCalled();

    component.reAuthForm.setValue({ username: 'alvaro', password: 'pass' });
    component.onReAuthSubmit();
    fixture.detectChanges();

    expect(authSpy.setToken).toHaveBeenCalledWith('new-token');
    expect(apiSpy.addNames).toHaveBeenCalledTimes(2);
    expect(localStorageSpy.clearListCache).toHaveBeenCalledWith('1');
    expect(apiSpy.finishAddition).toHaveBeenCalledTimes(1);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    expect(component.submitting()).toBeFalse();
  });

  it('FR-47: should fetch the list state on entry and redirect when the phase is not ADDITION', () => {
    apiSpy.getActiveList.and.returnValue(of({ ...LIST, phase: 'SELECTION' }));

    const freshFixture = TestBed.createComponent(SuggestionComponent);
    freshFixture.detectChanges();

    expect(apiSpy.getActiveList).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'selection']);
  });

  it('FR-51: should manage the name input through a reactive form control', () => {
    component.nameControl.setValue('Morena');
    fixture.detectChanges();

    expect(component.nameControl.value).toBe('Morena');

    component.nameControl.setValue('Pablo');
    component.addName(component.nameControl.value ?? '');

    expect(component.names()).toEqual(['Pablo']);
    expect(localStorageSpy.setItem).toHaveBeenCalledWith('1', 'suggestions', ['Pablo']);
  });
});