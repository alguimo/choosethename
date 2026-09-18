import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardComponent } from '@app/features/dashboard/dashboard.component';
import { ApiService } from '@app/services/api.service';
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
  members: [{ id: 'u1', username: 'alvaro' }],
};

const NOT_FOUND = () =>
  throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found', error: {} }));

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getActiveList', 'createList', 'joinList']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, ReactiveFormsModule],
      providers: [
        provideHttpClient(),
        provideAnimationsAsync(),
        { provide: ApiService, useValue: apiSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('TS-3: should display the active list card with the correct phase and members', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('ui-list-card') as HTMLElement;
    expect(card).toBeTruthy();
    expect(card.textContent).toContain('La pandilla');
    expect(card.textContent).toContain('Propuestas');
    expect(card.textContent).toContain('1 miembro');
  });

  it('TS-4: should display the empty state with create and join actions when there is no active list', () => {
    apiSpy.getActiveList.and.returnValue(NOT_FOUND());
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('ui-list-card')).toBeFalsy();
    expect(text).toContain('Crear lista nueva');
    expect(text).toContain('Unirse con código');
  });

  it('should show a generic error when fetching the active list fails', () => {
    apiSpy.getActiveList.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: {} })),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ha habido un error, inténtelo de nuevo');
  });

  it('TS-5: should navigate to the suggestion phase when a list is created', () => {
    apiSpy.getActiveList.and.returnValue(NOT_FOUND());
    fixture.detectChanges();

    component.openCreateModal();
    fixture.detectChanges();
    expect(component.showCreateModal()).toBeTrue();
    expect(fixture.nativeElement.querySelector('ui-modal')).toBeTruthy();

    component.createForm.setValue({ name: 'La pandilla' });
    apiSpy.createList.and.returnValue(of(LIST));
    component.createList();

    expect(apiSpy.createList).toHaveBeenCalledWith({ name: 'La pandilla' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'suggestion']);
    expect(component.showCreateModal()).toBeFalse();
  });

  it('TS-6: should display the backend error inline when creation fails', () => {
    apiSpy.getActiveList.and.returnValue(NOT_FOUND());
    fixture.detectChanges();

    component.openCreateModal();
    component.createForm.setValue({ name: 'La pandilla' });
    apiSpy.createList.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { error: 'Ya tienes una lista activa' } }),
      ),
    );
    component.createList();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ya tienes una lista activa');
    expect(component.showCreateModal()).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('TS-7: should navigate to the suggestion phase when joining with a valid code', () => {
    apiSpy.getActiveList.and.returnValue(NOT_FOUND());
    fixture.detectChanges();

    component.openJoinModal();
    fixture.detectChanges();
    expect(component.showJoinModal()).toBeTrue();

    component.joinForm.setValue({ code: 'ABC123' });
    apiSpy.joinList.and.returnValue(of(LIST));
    component.joinList();

    expect(apiSpy.joinList).toHaveBeenCalledWith({ code: 'ABC123' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'suggestion']);
    expect(component.showJoinModal()).toBeFalse();
  });

  it('TS-8: should display the backend error inline when the join code is invalid', () => {
    apiSpy.getActiveList.and.returnValue(NOT_FOUND());
    fixture.detectChanges();

    component.openJoinModal();
    component.joinForm.setValue({ code: 'XXXXXX' });
    apiSpy.joinList.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            error: { error: 'Código de invitación no válido' },
          }),
      ),
    );
    component.joinList();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Código de invitación no válido');
    expect(component.showJoinModal()).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('FR-9: should re-fetch the list state and navigate to the phase view when the card is clicked', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));
    fixture.detectChanges();
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(2);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'suggestion']);
  });

  it('FR-9: should navigate to the selection phase when the list is in SELECTION', () => {
    apiSpy.getActiveList.and.returnValue(of({ ...LIST, phase: 'SELECTION' }));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'selection']);
  });

  it('FR-9: should navigate to the voting phase when the list is in VOTING', () => {
    apiSpy.getActiveList.and.returnValue(of({ ...LIST, phase: 'VOTING' }));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'vote']);
  });

  it('FR-9: should navigate to the results view when the list is COMPLETED', () => {
    apiSpy.getActiveList.and.returnValue(of({ ...LIST, phase: 'COMPLETED' }));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'results']);
  });

  it('FR-16: should reuse the cached active list across visits within the TTL', () => {
    apiSpy.getActiveList.and.returnValue(of(LIST));
    fixture.detectChanges();
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);

    component.loadActiveList();
    expect(apiSpy.getActiveList).toHaveBeenCalledTimes(1);
    expect(component.activeList()?.id).toBe('1');
  });
});