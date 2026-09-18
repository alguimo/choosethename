import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiRoundIndicatorComponent } from '@app/ui-kit/molecules/round-indicator/round-indicator.component';

describe('UiRoundIndicatorComponent', () => {
  let fixture: ComponentFixture<UiRoundIndicatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiRoundIndicatorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiRoundIndicatorComponent);
    fixture.detectChanges();
  });

  // TS-12: displays "Ronda X de Y" text (FR-29).
  it('should render the "Ronda X de Y" text', () => {
    fixture.componentRef.setInput('currentRound', 2);
    fixture.componentRef.setInput('totalRounds', 5);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ronda 2 de 5');
  });

  it('should render a progress bar reflecting the current round', () => {
    fixture.componentRef.setInput('currentRound', 2);
    fixture.componentRef.setInput('totalRounds', 4);
    fixture.detectChanges();

    const fill = fixture.nativeElement.querySelector(
      '.ui-round-indicator__fill',
    ) as HTMLElement;

    expect(fill.style.width).toBe('50%');
  });

  it('should clamp the progress bar to 100% when past the last round', () => {
    fixture.componentRef.setInput('currentRound', 9);
    fixture.componentRef.setInput('totalRounds', 5);
    fixture.detectChanges();

    const fill = fixture.nativeElement.querySelector(
      '.ui-round-indicator__fill',
    ) as HTMLElement;

    expect(fill.style.width).toBe('100%');
  });
});