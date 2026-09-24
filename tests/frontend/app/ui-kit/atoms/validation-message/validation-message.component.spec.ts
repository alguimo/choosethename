import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiValidationMessageComponent, UiValidationType } from '@app/ui-kit/atoms/validation-message/validation-message.component';

const EXPECTED_COLOR: Record<UiValidationType, string> = {
  error: 'rgb(229, 115, 115)',
  warning: 'rgb(255, 183, 77)',
  info: 'rgb(79, 195, 247)',
};

describe('UiValidationMessageComponent', () => {
  let fixture: ComponentFixture<UiValidationMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiValidationMessageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiValidationMessageComponent);
    fixture.detectChanges();
  });

  // TS-17: renders without errors when minimal inputs are provided.
  it('should render an empty message when no inputs are provided', () => {
    expect(fixture.nativeElement.querySelector('.ui-validation-message')).toBeTruthy();
  });

  it('should render the message text', () => {
    fixture.componentRef.setInput('message', 'El nombre ya existe');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('El nombre ya existe');
  });

  // TS-7: renders correct icon and color for error, warning and info types (FR-20).
  it('should render the matching icon and color for each type', () => {
    const message = fixture.nativeElement.querySelector(
      '.ui-validation-message',
    ) as HTMLElement;

    for (const [variant, expectedColor] of Object.entries(EXPECTED_COLOR) as [
      UiValidationType,
      string,
    ][]) {
      fixture.componentRef.setInput('type', variant);
      fixture.detectChanges();

      expect(message.textContent).toContain(
        variant === 'error' ? 'error' : variant === 'warning' ? 'warning' : 'info',
      );
      expect(getComputedStyle(message).color).toBe(expectedColor);
    }
  });

  it('should expose the matching accessibility role for each type', () => {
    fixture.componentRef.setInput('message', 'Algo');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.getAttribute('role')).toBe('alert');

    fixture.componentRef.setInput('type', 'warning');
    fixture.detectChanges();
    expect(host.getAttribute('role')).toBe('status');

    fixture.componentRef.setInput('type', 'info');
    fixture.detectChanges();
    expect(host.getAttribute('role')).toBe('status');
  });
});