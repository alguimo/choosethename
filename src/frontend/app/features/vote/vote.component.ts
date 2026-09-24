import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { LocalStorageService } from '../../services/local-storage.service';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';
import { UiInputFieldComponent } from '../../ui-kit/atoms/input-field/input-field.component';
import { UiModalComponent } from '../../ui-kit/organisms/modal/modal.component';
import { UiRoundIndicatorComponent } from '../../ui-kit/molecules/round-indicator/round-indicator.component';
import { UiDraggableRankingListComponent } from '../../ui-kit/organisms/draggable-ranking-list/draggable-ranking-list.component';

@Component({
  selector: 'app-vote',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    UiButtonComponent,
    UiValidationMessageComponent,
    UiInputFieldComponent,
    UiModalComponent,
    UiRoundIndicatorComponent,
    UiDraggableRankingListComponent,
  ],
  template: `
    <section class="vote">
      <h2 class="vote__title">Votación</h2>

      @if (loading()) {
        <p class="vote__loading">Cargando...</p>
      }

      @if (!loading()) {
        @if (waiting()) {
          <ui-validation-message type="info" [message]="waitingMessage" />
        }

        <div class="vote__ranking" [class.vote__ranking--invalid]="invalid()">
          <ui-round-indicator
            [currentRound]="roundNumber()"
            [totalRounds]="totalRounds()"
          />

          <ui-draggable-ranking-list
            [items]="ranking()"
            [disabled]="submitting() || waiting()"
            (rankingsChanged)="onRankingsChanged($event)"
          ></ui-draggable-ranking-list>
        </div>

        @if (submitError()) {
          <ui-validation-message type="error" [message]="submitError()!" />
        }

        @if (!localStorage.isAvailable()) {
          <ui-validation-message type="warning" [message]="persistenceWarning" />
        }

        @if (pool().length > 0) {
          <ui-button
            label="Enviar voto"
            [disabled]="submitting() || waiting()"
            [loading]="submitting()"
            (clicked)="submitVote()"
          ></ui-button>
        }
      }

      <ui-modal title="Sesión caducada" [visible]="showReAuth()" (closed)="closeReAuth()">
        <p class="vote__reauth-hint">
          Tu sesión ha caducado. Introduce tus credenciales para enviar tu voto.
        </p>
        <form [formGroup]="reAuthForm" (ngSubmit)="onReAuthSubmit()">
          <ui-input-field
            label="Usuario"
            formControlName="username"
            placeholder="Tu nombre de usuario"
          ></ui-input-field>
          <ui-input-field
            label="Contraseña"
            type="password"
            formControlName="password"
            placeholder="Tu contraseña"
          ></ui-input-field>
          @if (reAuthError()) {
            <ui-validation-message type="error" [message]="reAuthError()!" />
          }
          <ui-button
            label="Reintentar"
            [disabled]="reAuthForm.invalid || reAuthLoading()"
            [loading]="reAuthLoading()"
          ></ui-button>
        </form>
      </ui-modal>
    </section>
  `,
  styles: [
    `
      .vote {
        width: var(--ui-layout-page-width);
        margin: var(--ui-spacing-lg) auto;
        padding: var(--ui-spacing-md);
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
      }

      .vote__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xl);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
      }

      .vote__ranking {
        padding: var(--ui-spacing-sm);
        border: 2px solid transparent;
        border-radius: var(--ui-radius-md);
        transition: border-color var(--ui-transition-fast);
      }

      .vote__ranking--invalid {
        border-color: var(--ui-color-danger);
      }

      .vote__reauth-hint {
        margin: 0 0 var(--ui-spacing-sm);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface);
      }
    `,
  ],
})
export class VoteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly apiService = inject(ApiService);
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  protected readonly localStorage = inject(LocalStorageService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  private readonly listId = this.route.snapshot.paramMap.get('id') ?? '';

  readonly persistenceWarning =
    'Tu progreso no se guardará localmente. No cierres la página.';
  readonly waitingMessage = 'Esperando a que el resto complete la fase';
  readonly pool = signal<string[]>([]);
  readonly ranking = signal<string[]>([]);
  readonly roundNumber = signal(1);
  readonly totalRounds = signal(1);
  readonly loading = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly invalid = signal(false);
  readonly submitting = signal(false);
  readonly myStepCompleted = signal(false);
  readonly showReAuth = signal(false);
  readonly reAuthError = signal<string | null>(null);
  readonly reAuthLoading = signal(false);

  readonly waiting = computed(() => this.myStepCompleted());

  readonly reAuthForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    this.loadVoting();
  }

  loadVoting(): void {
    this.loading.set(true);
    this.submitError.set(null);

    this.apiService.getListById(this.listId).subscribe({
      next: (list) => {
        if (list.phase !== 'VOTING') {
          this.router.navigate(['/lists', list.id, this.viewForPhase(list.phase)]);
          return;
        }
        this.pool.set(list.currentPool);
        this.roundNumber.set(list.currentRound);
        this.totalRounds.set(list.totalRounds);
        this.myStepCompleted.set(list.myStepCompleted);
        this.ranking.set(this.resolveRanking());
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.submitError.set('Ha habido un error, inténtelo de nuevo');
      },
    });
  }

  onRankingsChanged(order: string[]): void {
    if (this.waiting()) return;
    this.ranking.set(order);
    this.invalid.set(false);
    this.submitError.set(null);
    this.persistRanking(order);
  }

  submitVote(): void {
    if (this.waiting() || this.submitting() || this.ranking().length === 0) return;

    this.submitting.set(true);
    this.submitError.set(null);
    this.invalid.set(false);

    this.apiService
      .submitVote(this.listId, {
        roundNumber: this.roundNumber(),
        rankings: this.ranking(),
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.localStorage.clearListCache(this.listId);
          this.dashboardService.invalidate();
          this.router.navigate(['/']);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleVoteError(error);
        },
      });
  }

  onReAuthSubmit(): void {
    if (this.reAuthLoading() || this.reAuthForm.invalid) return;

    this.reAuthLoading.set(true);
    this.reAuthError.set(null);

    this.apiService
      .login({
        username: this.reAuthForm.value.username ?? '',
        password: this.reAuthForm.value.password ?? '',
      })
      .subscribe({
        next: (response) => {
          this.authService.setToken(response.accessToken);
          this.reAuthLoading.set(false);
          this.showReAuth.set(false);
          this.reAuthForm.reset();
          this.submitVote();
        },
        error: () => {
          this.reAuthLoading.set(false);
          this.reAuthError.set('Ha habido un error, inténtelo de nuevo');
        },
      });
  }

  closeReAuth(): void {
    this.showReAuth.set(false);
    this.reAuthError.set(null);
  }

  private voteKey(): string {
    return `vote_round_${this.roundNumber()}`;
  }

  private viewForPhase(phase: string): string {
    switch (phase) {
      case 'SELECTION':
        return 'selection';
      case 'VOTING':
        return 'vote';
      case 'COMPLETED':
        return 'results';
      default:
        return 'suggestion';
    }
  }

  private resolveRanking(): string[] {
    const saved = this.localStorage.getItem<string[]>(this.listId, this.voteKey());
    const pool = this.pool();
    if (
      saved &&
      saved.length === pool.length &&
      new Set(saved).size === pool.length &&
      pool.every((name) => saved.includes(name))
    ) {
      return saved;
    }
    return pool;
  }

  private persistRanking(order: string[]): void {
    this.localStorage.setItem<string[]>(this.listId, this.voteKey(), order);
  }

  private handleVoteError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400 || error.status === 422) {
        this.invalid.set(true);
        this.submitError.set('Voto no válido. Revisa el orden de los nombres');
        return;
      }
      if (error.status === 401) {
        this.showReAuth.set(true);
        this.reAuthError.set(null);
        return;
      }
    }
    this.submitError.set('Ha habido un error, inténtelo de nuevo');
  }
}