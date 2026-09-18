import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { DashboardService } from '../../services/dashboard.service';
import { ApiError, NameEntry } from '../../models/api.models';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';

@Component({
  selector: 'app-selection',
  standalone: true,
  imports: [UiButtonComponent, UiValidationMessageComponent],
  template: `
    <section class="selection">
      <h2 class="selection__title">Selección</h2>

      @if (loading()) {
        <p class="selection__loading">Cargando...</p>
      }

      @if (error()) {
        <ui-validation-message type="error" [message]="error()!" />
      }

      @if (!loading() && !error()) {
        <div class="selection__section">
          <h3 class="selection__section-title">Nombres comunes</h3>
          <ul class="selection__list">
            @for (entry of commonNames(); track entry.normalizedName) {
              <li class="selection__item">{{ entry.name }}</li>
            } @empty {
              <li class="selection__item selection__item--empty">
                Aún no hay nombres comunes.
              </li>
            }
          </ul>
        </div>

        <div class="selection__section">
          <h3 class="selection__section-title">Sugerencias</h3>
          <ul class="selection__list">
            @for (entry of fadedSuggestions(); track entry.normalizedName) {
              <li class="selection__item">
                <button
                  type="button"
                  class="selection__faded-item"
                  [disabled]="adoptingName() === entry.name"
                  (click)="adopt(entry.name)"
                >
                  {{ entry.name }}
                </button>
              </li>
            } @empty {
              <li class="selection__item selection__item--empty">
                No hay sugerencias para adoptar.
              </li>
            }
          </ul>
        </div>

        <div class="selection__section">
          <h3 class="selection__section-title">Mis nombres</h3>
          <ul class="selection__list">
            @for (entry of myNames(); track entry.normalizedName) {
              <li class="selection__item">{{ entry.name }}</li>
            } @empty {
              <li class="selection__item selection__item--empty">
                Aún no tienes nombres.
              </li>
            }
          </ul>
        </div>

        @if (actionError()) {
          <ui-validation-message type="error" [message]="actionError()!" />
        }

        <ui-button
          label="Completar selección"
          [disabled]="completing()"
          [loading]="completing()"
          (clicked)="completeSelection()"
        ></ui-button>
      }
    </section>
  `,
  styles: [
    `
      .selection {
        max-width: 480px;
        margin: 2rem auto;
        padding: 1rem;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
      }

      .selection__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xl);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
      }

      .selection__section {
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-xs);
      }

      .selection__section-title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-lg);
        font-weight: var(--ui-font-weight-medium);
        color: var(--ui-color-on-surface);
      }

      .selection__list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-xs);
      }

      .selection__item {
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

      .selection__item--empty {
        justify-content: center;
        border-style: dashed;
        color: var(--ui-color-on-surface-variant);
      }

      .selection__faded-item {
        width: 100%;
        padding: 0;
        border: none;
        background: none;
        text-align: left;
        font-family: inherit;
        font-size: inherit;
        color: var(--ui-color-primary);
        cursor: pointer;
      }

      .selection__faded-item:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    `,
  ],
})
export class SelectionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly apiService = inject(ApiService);
  private readonly dashboardService = inject(DashboardService);
  private readonly router = inject(Router);

  private readonly listId = this.route.snapshot.paramMap.get('id') ?? '';

  readonly commonNames = signal<NameEntry[]>([]);
  readonly fadedSuggestions = signal<NameEntry[]>([]);
  readonly myNames = signal<NameEntry[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly adoptingName = signal<string | null>(null);
  readonly completing = signal(false);

  ngOnInit(): void {
    this.loadSelection();
  }

  loadSelection(): void {
    this.loading.set(true);
    this.error.set(null);

    this.apiService.getSelection(this.listId).subscribe({
      next: (response) => {
        this.commonNames.set(response.commonNames);
        this.fadedSuggestions.set(response.fadedSuggestions);
        this.myNames.set(response.myNames);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Ha habido un error, inténtelo de nuevo');
      },
    });
  }

  adopt(name: string): void {
    if (this.adoptingName()) return;

    this.adoptingName.set(name);
    this.actionError.set(null);

    this.apiService.adoptFadedName(this.listId, name).subscribe({
      next: () => {
        this.adoptingName.set(null);
        this.loadSelection();
      },
      error: (error: unknown) => {
        this.adoptingName.set(null);
        this.actionError.set(this.extractError(error));
      },
    });
  }

  completeSelection(): void {
    if (this.completing()) return;

    this.completing.set(true);
    this.actionError.set(null);

    this.apiService.completeSelection(this.listId).subscribe({
      next: () => {
        this.completing.set(false);
        this.dashboardService.invalidate();
        this.router.navigate(['/']);
      },
      error: (error: unknown) => {
        this.completing.set(false);
        this.actionError.set(this.extractError(error));
      },
    });
  }

  private extractError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiError | undefined;
      if (body?.error) {
        return body.error;
      }
    }
    return 'Ha habido un error, inténtelo de nuevo';
  }
}
