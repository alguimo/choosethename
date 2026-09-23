import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { DashboardComponent } from '@app/features/dashboard/dashboard.component';
import { UiCopyFieldComponent } from '@app/ui-kit/molecules/copy-field/copy-field.component';
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
  myStepCompleted: false,
};

const SECOND: ListResponse = { ...LIST, id: '2', name: 'Otra lista' };

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getMyLists', 'createList', 'joinList']);
    apiSpy.getMyLists.and.returnValue(of([]));
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
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('TS-3: should display one card per list with the correct phase and members', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST, SECOND]));
    fixture.detectChanges();

    const cards = fixture.nativeElement.querySelectorAll('ui-list-card') as NodeListOf<HTMLElement>;
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('La pandilla');
    expect(cards[0].textContent).toContain('Propuestas');
    expect(cards[0].textContent).toContain('1 miembro');
    expect(cards[1].textContent).toContain('Otra lista');
  });

  it('TS-4: should display the empty state with create and join actions when there are no lists', () => {
    apiSpy.getMyLists.and.returnValue(of([]));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('ui-list-card')).toBeFalsy();
    expect(text).toContain('No tienes ninguna lista');
    expect(text).toContain('Crear lista nueva');
    expect(text).toContain('Unirse con código');
  });

  it('TS-37: should keep the create and join actions visible when there are lists', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST, SECOND]));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Crear lista nueva');
    expect(text).toContain('Unirse con código');
  });

  it('should show a generic error when fetching the lists fails', () => {
    apiSpy.getMyLists.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: {} })),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ha habido un error, inténtelo de nuevo');
  });

  it('TS-5: should navigate to the suggestion phase when a list is created', () => {
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
    fixture.detectChanges();

    component.openCreateModal();
    component.createForm.setValue({ name: 'La pandilla' });
    apiSpy.createList.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { error: 'Nombre no válido' } })),
    );
    component.createList();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nombre no válido');
    expect(component.showCreateModal()).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('TS-7: should navigate to the suggestion phase when joining with a valid code', () => {
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

  it('FR-9: should navigate to the suggestion phase when an ADDITION card is clicked', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'suggestion']);
  });

  it('FR-9: should navigate to the selection phase when the list is in SELECTION', () => {
    apiSpy.getMyLists.and.returnValue(of([{ ...LIST, phase: 'SELECTION' }]));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'selection']);
  });

  it('FR-9: should navigate to the voting phase when the list is in VOTING', () => {
    apiSpy.getMyLists.and.returnValue(of([{ ...LIST, phase: 'VOTING' }]));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'vote']);
  });

  it('FR-9: should navigate to the results view when the list is COMPLETED', () => {
    apiSpy.getMyLists.and.returnValue(of([{ ...LIST, phase: 'COMPLETED' }]));
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '1', 'results']);
  });

  it('FR-16: should reuse the cached lists across visits within the TTL', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));
    fixture.detectChanges();
    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);

    component.loadLists();
    expect(apiSpy.getMyLists).toHaveBeenCalledTimes(1);
    expect(component.lists().length).toBe(1);
  });

  it('TS-40: should navigate to the new list when a second list is created', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));
    fixture.detectChanges();

    component.openCreateModal();
    component.createForm.setValue({ name: 'Otra lista' });
    apiSpy.createList.and.returnValue(of(SECOND));
    component.createList();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/lists', '2', 'suggestion']);
  });

  it('TS-41: should open the invitation modal with the list code when Invitar is activated', () => {
    apiSpy.getMyLists.and.returnValue(of([LIST]));
    fixture.detectChanges();

    component.openInviteModal(LIST);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Invitar a La pandilla');
    expect(text).toContain('Código de invitación');
    expect(fixture.nativeElement.querySelector('ui-copy-field input')?.value).toBe('ABC123');
  });

  it('TS-42: should close the invitation modal and clear the target', () => {
    component.openInviteModal(LIST);
    fixture.detectChanges();
    expect(component.invitationTarget()).toEqual(LIST);

    component.closeInviteModal();
    fixture.detectChanges();

    expect(component.invitationTarget()).toBeNull();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('ui-copy-field input'),
    ).toBeNull();
  });

  it('FR-50: should show the copied confirmation when the invite code is copied', () => {
    component.openInviteModal(LIST);
    fixture.detectChanges();

    const copyField = fixture.debugElement.query(
      By.directive(UiCopyFieldComponent),
    ).componentInstance as UiCopyFieldComponent;
    copyField.copied.emit();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Código copiado');
  });

  it('FR-49: should show the manual copy message when the clipboard copy fails', () => {
    component.openInviteModal(LIST);
    fixture.detectChanges();

    const copyField = fixture.debugElement.query(
      By.directive(UiCopyFieldComponent),
    ).componentInstance as UiCopyFieldComponent;
    copyField.copyFailed.emit();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'No se ha podido copiar automáticamente. Copia el código manualmente.',
    );
  });
});
