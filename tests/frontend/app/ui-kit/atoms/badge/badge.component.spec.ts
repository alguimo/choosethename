import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiBadgeComponent } from '@app/ui-kit/atoms/badge/badge.component';

type ColorVariant = 'default' | 'success' | 'warning' | 'danger';

const COLOR_BACKGROUND: Record<ColorVariant, string> = {
  default: 'rgb(38, 40, 51)',
  success: 'rgb(30, 58, 36)',
  warning: 'rgb(58, 46, 30)',
  danger: 'rgb(58, 30, 30)',
};

describe('UiBadgeComponent', () => {
  let fixture: ComponentFixture<UiBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiBadgeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiBadgeComponent);
    fixture.detectChanges();
  });

  // TS-17: renders without errors when minimal inputs are provided.
  it('should render an empty badge when no inputs are provided', () => {
    expect(fixture.nativeElement.querySelector('.ui-badge')).toBeTruthy();
  });

  it('should render the label text', () => {
    fixture.componentRef.setInput('label', 'Segunda ronda');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Segunda ronda');
  });

  // TS-6: renders correct background color for each color variant (FR-18).
  it('should apply the matching background color for each color variant', () => {
    const badge = fixture.nativeElement.querySelector('.ui-badge') as HTMLElement;

    for (const [variant, expectedColor] of Object.entries(COLOR_BACKGROUND) as [
      ColorVariant,
      string,
    ][]) {
      fixture.componentRef.setInput('color', variant);
      fixture.detectChanges();

      expect(getComputedStyle(badge).backgroundColor).toBe(expectedColor);
    }
  });
});