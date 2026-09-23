import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { AdminComponent } from '@app/features/admin/admin.component';
import { ApiService } from '@app/services/api.service';
import { UserProfile } from '@app/models/api.models';

const ADMIN_USER: UserProfile = { id: 1, username: 'alvaro', role: 'ADMIN' };
const PARTICIPANT_USER: UserProfile = { id: 2, username: 'ana', role: 'PARTICIPANT' };

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getAdminUsers', 'resetUserPassword']);
    apiSpy.getAdminUsers.and.returnValue(of([ADMIN_USER, PARTICIPANT_USER]));

    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        provideHttpClient(),
        provideAnimationsAsync(),
        { provide: ApiService, useValue: apiSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('TS-46: should call GET /admin/users and render each user with id, username and role', () => {
    expect(apiSpy.getAdminUsers).toHaveBeenCalled();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('alvaro');
    expect(element.textContent).toContain('ana');
    expect(element.textContent).toContain('ADMIN');
    expect(element.textContent).toContain('PARTICIPANT');
    expect(component.users().length).toBe(2);
  });

  it('TS-47: should reset a password successfully, close the modal and show the success message', () => {
    apiSpy.resetUserPassword.and.returnValue(of(undefined));

    component.openReset(PARTICIPANT_USER);
    fixture.detectChanges();

    component.resetForm.controls['newPassword'].setValue('NuevaPass1');
    component.resetForm.markAsTouched();
    fixture.detectChanges();

    component.submitReset();
    fixture.detectChanges();

    expect(apiSpy.resetUserPassword).toHaveBeenCalledWith(2, 'NuevaPass1');
    expect(component.resetTarget()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Contraseña actualizada');
  });

  it('TS-47: should not send the request when the new password is invalid', () => {
    component.openReset(PARTICIPANT_USER);
    fixture.detectChanges();

    component.resetForm.controls['newPassword'].setValue('weak');
    component.resetForm.controls['newPassword'].markAsTouched();
    fixture.detectChanges();

    expect(component.passwordError()).toContain('8 caracteres');

    component.submitReset();

    expect(apiSpy.resetUserPassword).not.toHaveBeenCalled();
    expect(component.resetTarget()).not.toBeNull();
  });

  it('FR-77: should show the backend error inline and keep the modal open on 400', () => {
    apiSpy.resetUserPassword.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { error: 'Password must contain at least one uppercase letter' },
          }),
      ),
    );

    component.openReset(PARTICIPANT_USER);
    fixture.detectChanges();

    component.resetForm.controls['newPassword'].setValue('NuevaPass1');
    component.resetForm.controls['newPassword'].markAsTouched();
    fixture.detectChanges();

    component.submitReset();
    fixture.detectChanges();

    expect(component.resetError()).toContain('uppercase');
    expect(component.resetTarget()).not.toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Contraseña actualizada');
  });

  it('FR-77: should keep the modal open and show the backend error on 404', () => {
    apiSpy.resetUserPassword.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 404, error: { error: 'User not found' } }),
      ),
    );

    component.openReset(PARTICIPANT_USER);
    fixture.detectChanges();

    component.resetForm.controls['newPassword'].setValue('NuevaPass1');
    fixture.detectChanges();

    component.submitReset();
    fixture.detectChanges();

    expect(component.resetError()).toContain('User not found');
    expect(component.resetTarget()).not.toBeNull();
  });
});