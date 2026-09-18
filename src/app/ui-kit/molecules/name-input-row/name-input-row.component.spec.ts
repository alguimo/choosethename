import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { UiNameInputRowComponent } from './name-input-row.component';
import { FormsModule } from '@angular/forms';

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
      imports: [UiNameInputRowComponent, FormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(UiNameInputRowComponent);
    fixture.detectChanges();
  });

  it('should render an input and a send button', () => {
    expect(inputElement()).toBeTruthy();
    expect(fixture.nativeElement.querySelector('ui-icon-button')).toBeTruthy();
  });

  it('should emit the trimmed value on Enter', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    typeIn('  Pablo  ');
    pressEnter();

    expect(spy).toHaveBeenCalledOnceWith('Pablo');
  });

  it('should emit the value when the send button is clicked', () => {
    const spy = jasmine.createSpy('nameSubmitted');
    fixture.componentInstance.nameSubmitted.subscribe(spy);

    typeIn('Rivendell');

    const sendButton = fixture.nativeElement.querySelector('button');
    sendButton.dispatchEvent(new Event('click', { bubbles: true }));

    expect(spy).toHaveBeenCalledOnceWith('Rivendell');
  });

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

  it('should lock the send button while a submission is in flight', fakeAsync(() => {
    const sendButton = fixture.nativeElement.querySelector('button') as HTMLButtonElement;

    typeIn('Camelot');
    pressEnter();
    fixture.detectChanges();

    expect(fixture.componentInstance.submitting()).toBeTrue();
    expect((inputElement() as HTMLInputElement).disabled).toBeFalse();
    expect(sendButton.disabled).toBeTrue();

    tick(300);
    fixture.detectChanges();

    expect(fixture.componentInstance.submitting()).toBeFalse();
    expect(fixture.componentInstance.currentValue()).toBe('');
    expect((inputElement() as HTMLInputElement).disabled).toBeFalse();
    expect(sendButton.disabled).toBeFalse();
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
