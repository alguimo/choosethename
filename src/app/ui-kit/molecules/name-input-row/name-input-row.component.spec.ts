import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { UiNameInputRowComponent } from './name-input-row.component';

describe('UiNameInputRowComponent', () => {
  let fixture: ComponentFixture<UiNameInputRowComponent>;

  function inputElement(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input') as HTMLInputElement;
  }

  function pressEnter(): void {
    inputElement().dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }),
    );
  }

  function typeIn(value: string): void {
    const input = inputElement();
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiNameInputRowComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiNameInputRowComponent);
    fixture.detectChanges();
  });

  // TS-17: renders without errors when minimal inputs are provided.
  it('should render an input and a send button', () => {
    expect(inputElement()).toBeTruthy();
    expect(fixture.nativeElement.querySelector('ui-icon-button')).toBeTruthy();
  });

  // TS-8: emits nameSubmitted with the trimmed value on Enter (FR-22).
  it('should emit the trimmed value on Enter', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    typeIn('  Pablo  ');
    pressEnter();

    expect(spy).toHaveBeenCalledOnceWith('Pablo');
  });

  // TS-8: emits nameSubmitted when the send button is activated.
  it('should emit the value when the send button is clicked', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    typeIn('Rivendell');

    const sendButton = fixture.nativeElement.querySelector('button');
    sendButton.dispatchEvent(new Event('click', { bubbles: true }));

    expect(spy).toHaveBeenCalledOnceWith('Rivendell');
  });

  // TS-9: prevents nameSubmitted when input is empty or whitespace-only (FR-23).
  it('should not emit when the input is empty', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    pressEnter();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit when the input contains only whitespace', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    typeIn('   \t \n  ');
    pressEnter();

    expect(spy).not.toHaveBeenCalled();
  });

  // Edge case: rapid submissions are locked for 300ms.
  it('should clear the field and block a second submission within 300ms', fakeAsync(() => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    typeIn('Camelot');
    pressEnter();
    tick(100);
    pressEnter();

    expect(spy).toHaveBeenCalledTimes(1);
    tick(300);
    fixture.detectChanges();

    expect(fixture.componentInstance.currentValue()).toBe('');
  }));

  it('should not emit when disabled', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    typeIn('Eldoria');
    pressEnter();

    expect(spy).not.toHaveBeenCalled();
  });
});