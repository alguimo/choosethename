import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiInputFieldComponent } from './input-field.component';
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
});
