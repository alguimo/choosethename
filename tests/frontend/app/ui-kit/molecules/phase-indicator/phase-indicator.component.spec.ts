import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiPhaseIndicatorComponent } from '@app/ui-kit/molecules/phase-indicator/phase-indicator.component';

describe('UiPhaseIndicatorComponent', () => {
  let fixture: ComponentFixture<UiPhaseIndicatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiPhaseIndicatorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiPhaseIndicatorComponent);
    fixture.detectChanges();
  });

  // TS-11: highlights the correct current phase (FR-27).
  it('should mark the current step circle with the current class and aria-current', () => {
    fixture.componentRef.setInput('totalPhases', 4);
    fixture.componentRef.setInput('currentPhase', 2);
    fixture.componentRef.setInput('phase', 'SELECTION');
    fixture.detectChanges();

    const steps = fixture.nativeElement.querySelectorAll(
      '.ui-phase-indicator__step',
    ) as NodeListOf<HTMLElement>;

    expect(steps.length).toBe(4);
    expect(steps[1].classList.contains('ui-phase-indicator__step--current')).toBeTrue();
    expect(steps[1].querySelector('.ui-phase-indicator__circle')?.getAttribute('aria-current')).toBe('step');
    expect(steps[0].classList.contains('ui-phase-indicator__step--completed')).toBeTrue();
    expect(steps[2].classList.contains('ui-phase-indicator__step--current')).toBeFalse();
  });

  it('should display the phase label under the current step', () => {
    fixture.componentRef.setInput('totalPhases', 4);
    fixture.componentRef.setInput('currentPhase', 3);
    fixture.componentRef.setInput('phase', 'VOTING');
    fixture.detectChanges();

    const labels = fixture.nativeElement.querySelectorAll(
      '.ui-phase-indicator__label',
    ) as NodeListOf<HTMLElement>;

    expect(labels.length).toBe(1);
    expect(labels[0].textContent).toContain('Votación');
  });

  it('should render a connecting line between steps and mark active lines', () => {
    fixture.componentRef.setInput('totalPhases', 4);
    fixture.componentRef.setInput('currentPhase', 2);
    fixture.detectChanges();

    const lines = fixture.nativeElement.querySelectorAll(
      '.ui-phase-indicator__line',
    ) as NodeListOf<HTMLElement>;

    expect(lines.length).toBe(3);
    expect(lines[0].classList.contains('ui-phase-indicator__line--active')).toBeTrue();
    expect(lines[1].classList.contains('ui-phase-indicator__line--active')).toBeFalse();
  });
});