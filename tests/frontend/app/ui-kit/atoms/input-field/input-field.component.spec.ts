import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiInputFieldComponent } from '@app/ui-kit/atoms/input-field/input-field.component';
import { ReactiveFormsModule } from '@angular/forms';

describe('UiInputFieldComponent', () => {
  let fixture: ComponentFixture<UiInputFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiInputFieldComponent, ReactiveFormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(UiInputFieldComponent);
    fixture.detectChanges();
  });

  it('should render an input when no inputs are provided', () => {
    expect(fixture.nativeElement.querySelector('input')).toBeTruthy();
  });

  it('should render the label linked to the input', () => {
    fixture.componentRef.setInput('label', 'Nombre');
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(label.textContent).toContain('Nombre');
    expect(label.getAttribute('for')).toBe(input.id);
  });

  it('should apply the error state and show the error message when error is set', () => {
    fixture.componentRef.setInput('error', 'El nombre ya existe');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    const error = fixture.nativeElement.querySelector('.ui-input-field__error') as HTMLElement;

    expect(input.classList.contains('ui-input-field__control--error')).toBeTrue();
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(error).toBeTruthy();
    expect(error.textContent).toContain('El nombre ya existe');
    expect(input.getAttribute('aria-describedby')).toBe(error.id);
  });

  it('should respect the maxLength bound to the input', () => {
    fixture.componentRef.setInput('maxLength', 20);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.getAttribute('maxlength')).toBe('20');
  });

  it('TS-3: should emit valueChanged on input and submitted on Enter', () => {
    const valueSpy = jasmine.createSpy('valueChanged');
    const submittedSpy = jasmine.createSpy('submitted');
    fixture.componentInstance.valueChanged.subscribe(valueSpy);
    fixture.componentInstance.submitted.subscribe(submittedSpy);

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'Morena';
    input.dispatchEvent(new Event('input'));

    expect(valueSpy).toHaveBeenCalledWith('Morena');

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    expect(submittedSpy).toHaveBeenCalledTimes(1);
  });

  it('TS-19: should render a visibility toggle for password inputs and flip the type on activation', () => {
    fixture.componentRef.setInput('type', 'password');
    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector(
      '.ui-input-field__toggle',
    ) as HTMLButtonElement;
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;

    expect(toggle).toBeTruthy();
    expect(toggle.getAttribute('aria-pressed')).toBe('false');
    expect(toggle.getAttribute('aria-label')).toBe('Mostrar contraseña');
    expect(input.getAttribute('type')).toBe('password');

    toggle.click();
    fixture.detectChanges();

    expect(input.getAttribute('type')).toBe('text');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar contraseña');
  });

  it('should render no toggle for text inputs', () => {
    fixture.componentRef.setInput('type', 'text');
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('.ui-input-field__toggle'),
    ).toBeNull();
  });
});
