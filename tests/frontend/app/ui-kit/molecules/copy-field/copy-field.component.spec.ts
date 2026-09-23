import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiCopyFieldComponent } from '@app/ui-kit/molecules/copy-field/copy-field.component';

describe('UiCopyFieldComponent', () => {
  let fixture: ComponentFixture<UiCopyFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiCopyFieldComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiCopyFieldComponent);
    fixture.detectChanges();
  });

  it('should render the label, the value and the copy button', () => {
    fixture.componentRef.setInput('label', 'Código de la lista');
    fixture.componentRef.setInput('value', 'ABC123');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(text).toContain('Código de la lista');
    expect(input.value).toBe('ABC123');
    expect(text).toContain('Copiar');
  });

  it('TS-22: should copy the value via the clipboard API and emit copied', async () => {
    const windowMock = Object.getOwnPropertyDescriptor(window, 'navigator');
    const clipboardMock = {
      writeText: jasmine.createSpy('writeText').and.resolveTo(),
    };
    Object.defineProperty(navigator, 'clipboard', {
      value: clipboardMock,
      configurable: true,
    });
    fixture.componentRef.setInput('value', 'ABC123');

    const copiedSpy = jasmine.createSpy('copied');
    const copyFailedSpy = jasmine.createSpy('copyFailed');
    fixture.componentInstance.copied.subscribe(copiedSpy);
    fixture.componentInstance.copyFailed.subscribe(copyFailedSpy);

    await fixture.componentInstance.copy();

    expect(clipboardMock.writeText).toHaveBeenCalledWith('ABC123');
    expect(copiedSpy).toHaveBeenCalledTimes(1);
    expect(copyFailedSpy).not.toHaveBeenCalled();

    Object.defineProperty(navigator, 'clipboard', windowMock ?? { writable: true });
  });

  it('should emit copyFailed and select the value when the clipboard API is unavailable', async () => {
    const windowMock = Object.getOwnPropertyDescriptor(window, 'navigator');
    Object.defineProperty(navigator, 'clipboard', {
      value: undefined,
      configurable: true,
    });
    fixture.componentRef.setInput('value', 'ABC123');

    const copiedSpy = jasmine.createSpy('copied');
    const copyFailedSpy = jasmine.createSpy('copyFailed');
    fixture.componentInstance.copied.subscribe(copiedSpy);
    fixture.componentInstance.copyFailed.subscribe(copyFailedSpy);

    await fixture.componentInstance.copy();
    fixture.detectChanges();

    expect(copyFailedSpy).toHaveBeenCalledTimes(1);
    expect(copiedSpy).not.toHaveBeenCalled();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.selectionEnd).toBeGreaterThan(0);

    Object.defineProperty(navigator, 'clipboard', windowMock ?? { writable: true });
  });
});