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
});
