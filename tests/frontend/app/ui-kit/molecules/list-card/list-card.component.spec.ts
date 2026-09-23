import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiListCardComponent } from '@app/ui-kit/molecules/list-card/list-card.component';

describe('UiListCardComponent', () => {
  let fixture: ComponentFixture<UiListCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiListCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiListCardComponent);
    fixture.detectChanges();
  });

  // TS-10: renders title, phase badge, and member count (FR-25).
  it('should render title, phase badge, and member count', () => {
    fixture.componentRef.setInput('title', 'Nombres para el gato');
    fixture.componentRef.setInput('phase', 'VOTING');
    fixture.componentRef.setInput('memberCount', 4);
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;
    expect(native.textContent).toContain('Nombres para el gato');
    expect(native.querySelector('ui-badge')?.textContent).toContain('Votación');
    expect(native.textContent).toContain('4 miembros');
  });

  it('should render an unknown phase with a fallback label', () => {
    fixture.componentRef.setInput('title', 'Lista rara');
    fixture.componentRef.setInput('phase', 'UNKNOWN');
    fixture.detectChanges();

    const native = fixture.nativeElement as HTMLElement;
    expect(native.querySelector('ui-badge')?.textContent).toContain('UNKNOWN');
  });

  it('should use the singular member label for a single member', () => {
    fixture.componentRef.setInput('title', 'Lista');
    fixture.componentRef.setInput('memberCount', 1);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('1 miembro');
  });

  it('should emit clicked when activated by click', () => {
    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement
      .querySelector('.ui-list-card')
      .dispatchEvent(new MouseEvent('click', { bubbles: true }));

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit clicked when activated with the Enter key', () => {
    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    fixture.nativeElement
      .querySelector('.ui-list-card')
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit clicked and prevent default when activated with the space key', () => {
    const spy = jasmine.createSpy('clicked');
    fixture.componentInstance.clicked.subscribe(spy);

    const event = new KeyboardEvent('keydown', { key: ' ', bubbles: true, cancelable: true });
    fixture.nativeElement.querySelector('.ui-list-card').dispatchEvent(event);

    expect(spy).toHaveBeenCalledTimes(1);
    expect(event.defaultPrevented).toBeTrue();
  });

  it('TS-20: should render the Invitar action only when invitations are open', () => {
    fixture.componentRef.setInput('invitationCode', 'ABC123');
    fixture.componentRef.setInput('invitationsOpen', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Invitar');

    fixture.componentRef.setInput('invitationsOpen', false);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Invitar');
  });

  it('should emit inviteClicked with the code without activating the card', () => {
    const inviteSpy = jasmine.createSpy('inviteClicked');
    const clickedSpy = jasmine.createSpy('clicked');
    fixture.componentInstance.inviteClicked.subscribe(inviteSpy);
    fixture.componentInstance.clicked.subscribe(clickedSpy);
    fixture.componentRef.setInput('invitationCode', 'ABC123');
    fixture.componentRef.setInput('invitationsOpen', true);
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ui-list-card') as HTMLElement;
    const inviteButton = fixture.nativeElement.querySelector('ui-button button') as HTMLButtonElement;

    inviteButton.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    card.dispatchEvent(new MouseEvent('click', { bubbles: true }));

    expect(inviteSpy).toHaveBeenCalledWith('ABC123');
    expect(clickedSpy).toHaveBeenCalledTimes(1);
  });
});