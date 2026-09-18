import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ResultEntry } from '../../models/api.models';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';

@Component({
  selector: 'app-results',
  standalone: true,
  imports: [UiButtonComponent, UiValidationMessageComponent],
  template: `
    <section class="results">
      <h2 class="results__title">Resultados</h2>

      @if (loading()) {
        <p class="results__loading">Cargando...</p>
      }

      @if (!loading()) {
        @if (notReady()) {
          <p class="results__processing">Los resultados están siendo procesados</p>
          <ui-button
            class="results__retry"
            label="Reintentar"
            (clicked)="loadResults()"
          ></ui-button>
        } @else if (error()) {
          <ui-validation-message type="error" [message]="error()!" />
        } @else {
          <ol class="results__list">
            @for (entry of topResults(); track entry.rank) {
              <li class="results__item">
                <span class="results__rank">{{ entry.rank }}</span>
                <span class="results__name">{{ entry.name }}</span>
                <span class="results__score">{{ entry.score }} puntos</span>
              </li>
            } @empty {
              <li class="results__item results__item--empty">Todavía no hay resultados.</li>
            }
          </ol>
        }
      }
    </section>
  `,
  styles: [
    `
      .results {
        max-width: 480px;
        margin: 2rem auto;
        padding: 1rem;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
      }

      .results__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xl);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
      }

      .results__processing {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        color: var(--ui-color-on-surface-variant);
      }

      .results__list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-xs);
      }

      .results__item {
        display: flex;
        align-items: center;
        gap: var(--ui-spacing-sm);
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline-variant);
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-surface);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        color: var(--ui-color-on-surface);
      }

      .results__item--empty {
        justify-content: center;
        border-style: dashed;
        color: var(--ui-color-on-surface-variant);
      }

      .results__rank {
        flex-shrink: 0;
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-primary);
      }

      .results__name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .results__score {
        flex-shrink: 0;
        color: var(--ui-color-on-surface-variant);
      }
    `,
  ],
})
export class ResultsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly apiService = inject(ApiService);

  private readonly listId = this.route.snapshot.paramMap.get('id') ?? '';

  readonly results = signal<ResultEntry[]>([]);
  readonly topResults = computed(() => this.results().slice(0, 3));
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly notReady = signal(false);

  ngOnInit(): void {
    this.loadResults();
  }

  loadResults(): void {
    this.loading.set(true);
    this.error.set(null);
    this.notReady.set(false);

    this.apiService.getResults(this.listId).subscribe({
      next: (response) => {
        this.results.set(response.results);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.notReady.set(true);
          return;
        }
        this.error.set('Ha habido un error, inténtelo de nuevo');
      },
    });
  }
}
