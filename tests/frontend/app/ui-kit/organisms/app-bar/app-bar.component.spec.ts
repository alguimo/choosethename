import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiAppBarComponent } from '@app/ui-kit/organisms/app-bar/app-bar.component';

describe('UiAppBarComponent', () => {
  let component: UiAppBarComponent;
  let fixture: ComponentFixture<UiAppBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiAppBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiAppBarComponent);
    component = fixture.componentInstance;
  });

  it('TS-18: should render the title and the logout label', () => {
    fixture.componentRef.setInput('title', 'Elegir el Nombre');
    fixture.componentRef.setInput('logoutLabel', 'Cerrar sesión');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Elegir el Nombre');
    expect(text).toContain('Cerrar sesión');
  });

  it('TS-18: should emit titleClicked when the title is clicked', () => {
    fixture.componentRef.setInput('title', 'Elegir el Nombre');
    fixture.detectChanges();

    let emitted = false;
    component.titleClicked.subscribe(() => (emitted = true));

    const title = (fixture.nativeElement as HTMLElement).querySelector(
      '.ui-app-bar__title',
    ) as HTMLButtonElement;
    title.click();

    expect(emitted).toBeTrue();
  });

  it('TS-18: should emit logoutClicked when the logout button is clicked', () => {
    fixture.componentRef.setInput('logoutLabel', 'Cerrar sesión');
    fixture.detectChanges();

    let emitted = false;
    component.logoutClicked.subscribe(() => (emitted = true));

    const button = (fixture.nativeElement as HTMLElement).querySelector(
      '.ui-button',
    ) as HTMLButtonElement;
    button.click();

    expect(emitted).toBeTrue();
  });

  it('TS-21: should render the user label when provided', () => {
    fixture.componentRef.setInput('userLabel', 'alvaro');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('alvaro');
  });

  it('TS-21: should not render the admin action when showAdmin is false', () => {
    fixture.componentRef.setInput('showAdmin', false);
    fixture.detectChanges();

    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.ui-button'),
    ).map((button) => (button.textContent ?? '').trim());

    expect(buttons).not.toContain('Admin');
  });

  it('TS-21: should render the admin action and emit adminClicked when showAdmin is true', () => {
    fixture.componentRef.setInput('showAdmin', true);
    fixture.detectChanges();

    let emitted = false;
    component.adminClicked.subscribe(() => (emitted = true));

    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.ui-button'),
    );
    const adminButton = buttons.find(
      (button) => (button.textContent ?? '').trim() === 'Admin',
    ) as HTMLButtonElement;

    expect(adminButton).toBeTruthy();
    adminButton.click();

    expect(emitted).toBeTrue();
  });
});
