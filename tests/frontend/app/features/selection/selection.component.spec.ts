import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SelectionComponent } from '@app/features/selection/selection.component';
import { ApiService } from '@app/services/api.service';
import { DashboardService } from '@app/services/dashboard.service';
import { SelectionResponse } from '@app/models/api.models';

const EMPTY_SELECTION: SelectionResponse = {
  commonNames: [],
  fadedSuggestions: [],
  myNames: [],
};

const SELECTION: SelectionResponse = {
  commonNames: [{ name: 'Ana', normalizedName: 'ana' }],
  fadedSuggestions: [{ name: 'Bruno', normalizedName: 'bruno' }],
  myNames: [{ name: 'Carla', normalizedName: 'carla' }],
};

describe('SelectionComponent', () => {
  let component: SelectionComponent;
  let fixture: ComponentFixture<SelectionComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let dashboardSpy: jasmine.SpyObj<DashboardService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', [
      'getSelection',
      'adoptFadedName',
      'completeSelection',
    ]);
    apiSpy.getSelection.and.returnValue(of(EMPTY_SELECTION));
    apiSpy.adoptFadedName.and.returnValue(of(undefined));
    apiSpy.completeSelection.and.returnValue(of(undefined));
    dashboardSpy = jasmine.createSpyObj('DashboardService', ['invalidate']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [SelectionComponent],
      providers: [
        { provide: ApiService, useValue: apiSpy },
        { provide: DashboardService, useValue: dashboardSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (): string | null => '1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectionComponent);
    component = fixture.componentInstance;
  });

  const renderedText = (): string =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('TS-16: should load the selection view with common, faded and own names', () => {
    apiSpy.getSelection.and.returnValue(of(SELECTION));

    fixture.detectChanges();

    expect(apiSpy.getSelection).toHaveBeenCalledWith('1');
    expect(component.commonNames()).toEqual(SELECTION.commonNames);
    expect(component.fadedSuggestions()).toEqual(SELECTION.fadedSuggestions);
    expect(component.myNames()).toEqual(SELECTION.myNames);
    expect(renderedText()).toContain('Nombres comunes');
    expect(renderedText()).toContain('Ana');
    expect(renderedText()).toContain('Sugerencias');
    expect(renderedText()).toContain('Bruno');
    expect(renderedText()).toContain('Mis nombres');
    expect(renderedText()).toContain('Carla');
  });

  it('FR-27: should show an error when the selection view cannot be loaded', () => {
    apiSpy.getSelection.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: {} })),
    );

    fixture.detectChanges();

    expect(component.error()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(renderedText()).toContain('Ha habido un error, inténtelo de nuevo');
  });

  it('TS-17: should adopt a faded name and refresh the selection view', () => {
    const refreshed: SelectionResponse = {
      commonNames: [{ name: 'Bruno', normalizedName: 'bruno' }],
      fadedSuggestions: [],
      myNames: SELECTION.myNames,
    };
    apiSpy.getSelection.and.returnValues(of(SELECTION), of(refreshed));

    fixture.detectChanges();
    expect(apiSpy.getSelection).toHaveBeenCalledTimes(1);

    const fadedItem = fixture.nativeElement.querySelector(
      '.selection__faded-item',
    ) as HTMLButtonElement;
    fadedItem.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(apiSpy.adoptFadedName).toHaveBeenCalledWith('1', 'Bruno');
    expect(apiSpy.getSelection).toHaveBeenCalledTimes(2);
    expect(component.fadedSuggestions()).toEqual([]);
    expect(component.commonNames()).toEqual(refreshed.commonNames);
    expect(renderedText()).toContain('Bruno');
  });

  it('FR-29: should show the backend error inline when adoption fails', () => {
    apiSpy.getSelection.and.returnValue(of(SELECTION));
    apiSpy.adoptFadedName.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { error: 'No se puede adoptar' } }),
      ),
    );

    fixture.detectChanges();
    component.adopt('Bruno');
    fixture.detectChanges();

    expect(apiSpy.getSelection).toHaveBeenCalledTimes(1);
    expect(component.actionError()).toBe('No se puede adoptar');
    expect(renderedText()).toContain('No se puede adoptar');
  });

  it('TS-18: should complete the selection and navigate to the dashboard', () => {
    apiSpy.getSelection.and.returnValue(of(SELECTION));

    fixture.detectChanges();
    component.completeSelection();

    expect(apiSpy.completeSelection).toHaveBeenCalledWith('1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    expect(component.completing()).toBeFalse();
  });

  it('FR-17: should invalidate the dashboard cache after completing the selection', () => {
    apiSpy.getSelection.and.returnValue(of(SELECTION));

    fixture.detectChanges();
    component.completeSelection();

    expect(dashboardSpy.invalidate).toHaveBeenCalledTimes(1);
  });

  it('FR-31: should show an error and remain on the selection view when completing fails', () => {
    apiSpy.getSelection.and.returnValue(of(SELECTION));
    apiSpy.completeSelection.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { error: 'Fase incorrecta' } }),
      ),
    );

    fixture.detectChanges();
    component.completeSelection();
    fixture.detectChanges();

    expect(component.actionError()).toBe('Fase incorrecta');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(component.completing()).toBeFalse();
    expect(renderedText()).toContain('Fase incorrecta');
  });
});
