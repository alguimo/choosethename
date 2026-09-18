import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiIconButtonComponent } from '@app/ui-kit/atoms/icon-button/icon-button.component';

describe('UiIconButtonComponent', () => {
  let fixture: ComponentFixture<UiIconButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiIconButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiIconButtonComponent);
    fixture.detectChanges();
  });

  // TS-17: renders without errors when minimal inputs are provided.
  it('should render a button when no inputs are provided', () => {
    expect(fixture.nativeElement.querySelector('button')).toBeTruthy();
  });

  // TS-5: emits clicked on activation (FR-16).
  it('should emit clicked on click', () => {
    fixture.componentRef.setInput('icon', 'delete');
    fixture.detectChanges();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement.querySelector('button').click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  // FR-4: keyboard activation via Enter.
  it('should emit clicked on Enter key press', () => {
    fixture.componentRef.setInput('icon', 'delete');
    fixture.detectChanges();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement
      .querySelector('button')
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should render the Material icon', () => {
    fixture.componentRef.setInput('icon', 'edit');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('mat-icon')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('edit');
  });

  // NFR-3: icon-only button exposes the tooltip as an accessible name.
  it('should use the tooltip as aria-label for the icon-only button', () => {
    fixture.componentRef.setInput('icon', 'add');
    fixture.componentRef.setInput('tooltip', 'Añadir nombre');
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.getAttribute('aria-label')).toBe('Añadir nombre');
  });

  it('should not emit clicked when disabled', () => {
    fixture.componentRef.setInput('icon', 'delete');
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement.querySelector('button').click();

    expect(spy).not.toHaveBeenCalled();
  });
});