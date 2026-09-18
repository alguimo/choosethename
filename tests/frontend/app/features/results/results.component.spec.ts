import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ResultsComponent } from '@app/features/results/results.component';
import { ApiService } from '@app/services/api.service';
import { ResultsResponse } from '@app/models/api.models';

const RESULTS: ResultsResponse = {
  results: [
    { rank: 1, name: 'Ana', score: 10 },
    { rank: 2, name: 'Bruno', score: 8 },
    { rank: 3, name: 'Carla', score: 5 },
    { rank: 4, name: 'Diego', score: 2 },
  ],
};

describe('ResultsComponent', () => {
  let component: ResultsComponent;
  let fixture: ComponentFixture<ResultsComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getResults']);
    apiSpy.getResults.and.returnValue(of(RESULTS));

    await TestBed.configureTestingModule({
      imports: [ResultsComponent],
      providers: [
        { provide: ApiService, useValue: apiSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (): string | null => '1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResultsComponent);
    component = fixture.componentInstance;
  });

  const renderedText = (): string =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  const resultItems = () =>
    (fixture.nativeElement as HTMLElement).querySelectorAll('.results__item').length;

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('TS-24: should load and display the top-3 ranked names with scores', () => {
    fixture.detectChanges();

    expect(apiSpy.getResults).toHaveBeenCalledWith('1');
    expect(resultItems()).toBe(3);
    expect(renderedText()).toContain('Ana');
    expect(renderedText()).toContain('10');
    expect(renderedText()).toContain('Bruno');
    expect(renderedText()).toContain('Carla');
    expect(renderedText()).not.toContain('Diego');
  });

  it('FR-42: should not cache results and re-fetch on every load', () => {
    fixture.detectChanges();
    expect(apiSpy.getResults).toHaveBeenCalledTimes(1);

    component.loadResults();

    expect(apiSpy.getResults).toHaveBeenCalledTimes(2);
  });

  it('TS-25: should show a processing message with a retry button on 409', () => {
    apiSpy.getResults.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: {} })),
    );

    fixture.detectChanges();

    expect(component.notReady()).toBeTrue();
    expect(renderedText()).toContain('Los resultados están siendo procesados');
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.results__retry button'),
    ).toBeTruthy();

    apiSpy.getResults.and.returnValue(of(RESULTS));
    const retry = fixture.nativeElement.querySelector(
      '.results__retry button',
    ) as HTMLButtonElement;
    retry.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(apiSpy.getResults).toHaveBeenCalledTimes(2);
    expect(component.notReady()).toBeFalse();
    expect(renderedText()).toContain('Ana');
  });

  it('FR-40: should show a generic error when the results request fails', () => {
    apiSpy.getResults.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: {} })),
    );

    fixture.detectChanges();

    expect(component.error()).toBe('Ha habido un error, inténtelo de nuevo');
    expect(renderedText()).toContain('Ha habido un error, inténtelo de nuevo');
  });
});
