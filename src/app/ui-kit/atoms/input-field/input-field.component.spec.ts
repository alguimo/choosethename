import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiInputFieldComponent } from './input-field.component';

describe('UiInputFieldComponent', () => {
  let fixture: ComponentFixture<UiInputFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiInputFieldComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiInputFieldComponent);
    fixture.detectChanges();
  });

  // TS-17: renders without errors when minimal inputs are provided.
  it('should render an input when no inputs are provided', () => {
    expect(fixture.nativeElement.querySelector('input')).toBeTruthy();
  });

  // TS-3: emits valueChanged on input (FR-14).
  it('should emit valueChanged with the input value on input', () => {
    const spy = jasmine.createSpy('valueChanged');
    fixture.componentInstance.valueChanged.subscribe(spy);

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'Pablo';
    input.dispatchEvent(new Event('input', { bubbles: true }));

    expect(spy).toHaveBeenCalledWith('Pablo');
  });

  // TS-3: emits submitted on Enter (FR-14).
  it('should emit submitted on Enter key press', () => {
    const spy = jasmine.createSpy('submitted');
    fixture.componentInstance.submitted.subscribe(spy);

    fixture.nativeElement
      .querySelector('input')
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should render the label linked to the input', () => {
    fixture.componentRef.setInput('label', 'Nombre');
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(label.textContent).toContain('Nombre');
    expect(label.getAttribute('for')).toBe(input.id);
  });

  // TS-4: error set displays red border and error message (FR-13).
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

  it('should not emit valueChanged when disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const spy = jasmine.createSpy('valueChanged');
    fixture.componentInstance.valueChanged.subscribe(spy);

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'no';
    input.dispatchEvent(new Event('input', { bubbles: true }));

    expect(spy).not.toHaveBeenCalled();
  });
});