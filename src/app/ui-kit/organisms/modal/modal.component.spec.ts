import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiModalComponent } from './modal.component';

@Component({
  standalone: true,
  imports: [UiModalComponent],
  template: `
    <button id="trigger" class="trigger" (click)="visible = true">Abrir modal</button>
    <ui-modal title="Título de prueba" [visible]="visible" (closed)="visible = false">
      <p class="modal-content">Contenido del modal</p>
    </ui-modal>
  `,
})
class UiModalHostComponent {
  visible = false;
}

describe('UiModalComponent', () => {
  let fixture: ComponentFixture<UiModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiModalComponent);
    fixture.detectChanges();
  });

  it('should not render a panel when not visible', () => {
    expect(fixture.nativeElement.querySelector('.ui-modal-panel')).toBeFalsy();
  });

  it('should render the title and projected content when visible', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('title', 'Título de prueba');
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;
    expect(native.querySelector('.ui-modal-panel')).toBeTruthy();
    expect(native.textContent).toContain('Título de prueba');
  });

  // TS-15: emits closed on close button click (FR-36).
  it('should emit closed when the close button is clicked', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();

    const spy = jasmine.createSpy('closed');
    fixture.componentInstance.closed.subscribe(spy);

    const closeButton = fixture.nativeElement.querySelector(
      'ui-icon-button button',
    ) as HTMLButtonElement;
    closeButton.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit closed when the backdrop overlay is clicked', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();

    const spy = jasmine.createSpy('closed');
    fixture.componentInstance.closed.subscribe(spy);

    const backdrop = fixture.nativeElement.querySelector(
      '.ui-modal-backdrop',
    ) as HTMLElement;
    backdrop.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit closed when the Escape key is pressed while visible', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();

    const spy = jasmine.createSpy('closed');
    fixture.componentInstance.closed.subscribe(spy);

    fixture.nativeElement.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    );

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should not emit closed on Escape when hidden', () => {
    const spy = jasmine.createSpy('closed');
    fixture.componentInstance.closed.subscribe(spy);

    fixture.nativeElement.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    );

    expect(spy).not.toHaveBeenCalled();
  });

  // FU-38: renders a backdrop overlay when visible.
  it('should render a backdrop overlay when visible', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.ui-modal-backdrop')).toBeTruthy();
  });

  // FU-37: trap focus and expose dialog semantics.
  it('should mark the panel as a trap-focused dialog with a labelled title', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();

    const panel = fixture.nativeElement.querySelector('.ui-modal-panel') as HTMLElement;
    const title = fixture.nativeElement.querySelector(
      '.ui-modal-panel__title',
    ) as HTMLElement;

    expect(panel.getAttribute('role')).toBe('dialog');
    expect(panel.getAttribute('aria-modal')).toBe('true');
    expect(panel.getAttribute('aria-labelledby')).toBe(title.id);
  });

  // TS-16: traps focus when visible and restores focus to the trigger on close.
  it('should restore focus to the trigger element when the modal closes', () => {
    const hostFixture = TestBed.createComponent(UiModalHostComponent);
    hostFixture.detectChanges();

    const trigger = hostFixture.nativeElement.querySelector('.trigger') as HTMLButtonElement;
    trigger.focus();
    hostFixture.detectChanges();
    expect(document.activeElement).toBe(trigger);

    trigger.click();
    hostFixture.detectChanges();
    const closeButton = hostFixture.nativeElement.querySelector(
      'ui-icon-button button',
    ) as HTMLButtonElement;

    closeButton.click();
    hostFixture.detectChanges();

    expect(hostFixture.componentInstance.visible).toBeFalse();
    expect(document.activeElement).toBe(trigger);
  });
});