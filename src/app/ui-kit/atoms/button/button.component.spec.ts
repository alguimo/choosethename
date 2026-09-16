import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiButtonComponent } from './button.component';

describe('UiButtonComponent', () => {
  let fixture: ComponentFixture<UiButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiButtonComponent);
    fixture.detectChanges();
  });

  // TS-17: renders without errors when minimal inputs are provided.
  it('should render a button when no inputs are provided', () => {
    expect(fixture.nativeElement.querySelector('button')).toBeTruthy();
  });

  // TS-1: renders the label text.
  it('should render the label text', () => {
    fixture.componentRef.setInput('label', 'Guardar');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Guardar');
  });

  // TS-1: emits clicked on click (FR-11).
  it('should emit clicked when activated by click', () => {
    fixture.componentRef.setInput('label', 'Guardar');
    fixture.detectChanges();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement.querySelector('button').click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  // FR-11: emits clicked when activated by Enter key. (keyboard navigation FR-4)
  it('should emit clicked when activated by Enter key', () => {
    fixture.componentRef.setInput('label', 'Enviar');
    fixture.detectChanges();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement
      .querySelector('button')
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should not emit clicked when disabled', () => {
    fixture.componentRef.setInput('label', 'Enviar');
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement.querySelector('button').click();

    expect(spy).not.toHaveBeenCalled();
  });

  // TS-2: loading shows spinner and disables interaction (FR-10).
  it('should show a Material spinner and disable interaction when loading', () => {
    fixture.componentRef.setInput('label', 'Guardando');
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button');
    expect(button.disabled).toBeTrue();
    expect(button.querySelector('mat-spinner')).toBeTruthy();

    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);
    button.click();
    expect(spy).not.toHaveBeenCalled();
  });
});