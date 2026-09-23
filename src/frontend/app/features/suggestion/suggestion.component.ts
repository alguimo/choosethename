import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { switchMap } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { LocalStorageService } from '../../services/local-storage.service';
import { ListResponse, NameEntry } from '../../models/api.models';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiIconButtonComponent } from '../../ui-kit/atoms/icon-button/icon-button.component';
import { UiInputFieldComponent } from '../../ui-kit/atoms/input-field/input-field.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';
import { UiModalComponent } from '../../ui-kit/organisms/modal/modal.component';
import { UiNameInputRowComponent } from '../../ui-kit/molecules/name-input-row/name-input-row.component';
import { UiInviteModalComponent } from '../../ui-kit/molecules/invite-modal/invite-modal.component';

const NAME_PATTERN = /^[\p{L} -]+$/u;

function normalizeName(name: string): string {
  return name
    .trim()
    .toLocaleLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ');
}

@Component({
  selector: 'app-suggestion',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    UiButtonComponent,
    UiIconButtonComponent,
    UiInputFieldComponent,
    UiValidationMessageComponent,
    UiModalComponent,
    UiNameInputRowComponent,
    UiInviteModalComponent,
  ],
  template: `
    <section class="suggestion">
      <header class="suggestion__header">
        <h2 class="suggestion__title">Sugerencias</h2>
        <div class="suggestion__header-actions">
          @if (showInvite()) {
            <ui-button
              label="Invitar"
              variant="secondary"
              (clicked)="openInviteModal()"
            ></ui-button>
          }
          <ui-button
            label="Terminar Fase"
            [disabled]="submitting() || waiting()"
            [loading]="submitting()"
            (clicked)="completeAddition()"
          ></ui-button>
        </div>
      </header>

      @if (waiting()) {
        <ui-validation-message type="info" [message]="waitingMessage" />
        <ul class="suggestion__list">
          @for (entry of submittedNames(); track entry.normalizedName) {
            <li class="suggestion__item suggestion__item--readonly">
              <span class="suggestion__item-name">{{ entry.name }}</span>
            </li>
          } @empty {
            <li class="suggestion__item suggestion__item--empty">Aún no hay nombres.</li>
          }
        </ul>
      } @else {
        <ui-name-input-row
          label="Añade un nombre"
          placeholder="Ej. Morena"
          [formControl]="nameControl"
          (nameSubmitted)="addName($event)"
        ></ui-name-input-row>

        @if (!localStorage.isAvailable()) {
          <ui-validation-message type="warning" [message]="persistenceWarning" />
        }

        @if (inputError()) {
          <ui-validation-message type="error" [message]="inputError()!" />
        }
        @if (duplicateWarning()) {
          <ui-validation-message type="warning" [message]="duplicateWarning()!" />
        }

        <ul class="suggestion__list">
          @for (name of names(); track name; let i = $index) {
            <li class="suggestion__item">
              <span class="suggestion__item-name">{{ name }}</span>
              <ui-icon-button
                icon="delete"
                tooltip="Eliminar"
                variant="danger"
                (clicked)="removeName(i)"
              ></ui-icon-button>
            </li>
          } @empty {
            <li class="suggestion__item suggestion__item--empty">Aún no hay nombres.</li>
          }
        </ul>
      }

      @if (submitError()) {
        <ui-validation-message type="error" [message]="submitError()!" />
      }
    </section>

    <ui-modal title="Sesión caducada" [visible]="showReAuth()" (closed)="closeReAuth()">
      <p class="suggestion__reauth-hint">
        Tu sesión ha caducado. Introduce tus credenciales para terminar la fase.
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

    <ui-invite-modal
      [title]="'Invitar a ' + (list()?.name ?? '')"
      [code]="list()?.invitationCode ?? ''"
      [visible]="showInviteModal()"
      (closed)="closeInviteModal()"
    ></ui-invite-modal>
  `,
  styles: [
    `
      .suggestion {
        max-width: 480px;
        margin: 2rem auto;
        padding: 1rem;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
      }

      .suggestion__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--ui-spacing-sm);
      }

      .suggestion__header-actions {
        display: flex;
        align-items: center;
        gap: var(--ui-spacing-sm);
      }

      .suggestion__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xl);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
      }

      .suggestion__list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-xs);
      }

      .suggestion__item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--ui-spacing-sm);
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline-variant);
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-surface);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        color: var(--ui-color-on-surface);
      }

      .suggestion__item-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .suggestion__item--empty {
        justify-content: center;
        border-style: dashed;
        color: var(--ui-color-on-surface-variant);
      }

      .suggestion__item--readonly {
        color: var(--ui-color-on-surface-variant);
      }

      .suggestion__reauth-hint {
        margin: 0 0 var(--ui-spacing-sm);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface);
      }
    `,
  ],
})
export class SuggestionComponent implements OnInit {
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
  readonly list = signal<ListResponse | null>(null);
  readonly names = signal<string[]>([]);
  readonly submittedNames = signal<NameEntry[]>([]);
  readonly waiting = signal(false);
  readonly showInviteModal = signal(false);
  readonly inputError = signal<string | null>(null);
  readonly duplicateWarning = signal<string | null>(null);
  readonly submitError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly showReAuth = signal(false);
  readonly reAuthError = signal<string | null>(null);
  readonly reAuthLoading = signal(false);
  readonly nameControl = new FormControl<string>('');

  readonly showInvite = computed(
    () => this.list()?.invitationsOpen === true && !!this.list()?.invitationCode,
  );

  readonly reAuthForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    this.restoreLocalSuggestions();
    this.loadListState();
  }

  private loadListState(): void {
    this.apiService.getListById(this.listId).subscribe({
      next: (list) => {
        this.list.set(list);
        if (list.phase === 'ADDITION') {
          if (list.myStepCompleted) {
            this.waiting.set(true);
            this.loadSubmittedNames();
            return;
          }
          this.waiting.set(false);
          this.restoreLocalSuggestions();
          return;
        }
        this.router.navigate(['/lists', list.id, this.viewForPhase(list.phase)]);
      },
      error: () => {
        // Offline resilience (FR-4, FR-45): keep local editing available and
        // rely on re-authentication at phase completion.
        this.list.set(null);
        this.waiting.set(false);
        this.restoreLocalSuggestions();
      },
    });
  }

  private loadSubmittedNames(): void {
    this.apiService.getMyNames(this.listId).subscribe({
      next: (response) => this.submittedNames.set(response.names),
      error: () => this.submittedNames.set([]),
    });
  }

  private restoreLocalSuggestions(): void {
    this.names.set(this.localStorage.getItem<string[]>(this.listId, 'suggestions') ?? []);
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

  addName(name: string): void {
    if (this.waiting()) return;
    const trimmed = name.trim();
    this.inputError.set(null);
    this.duplicateWarning.set(null);

    if (!NAME_PATTERN.test(trimmed)) {
      this.inputError.set('Solo se permiten letras, espacios y guiones');
      return;
    }

    const normalized = normalizeName(trimmed);
    if (this.names().some((existing) => normalizeName(existing) === normalized)) {
      this.duplicateWarning.set('Este nombre ya está en la lista');
      return;
    }

    this.names.set([...this.names(), trimmed]);
    this.persist();
  }

  removeName(index: number): void {
    if (this.waiting()) return;
    const current = [...this.names()];
    if (index < 0 || index >= current.length) return;

    current.splice(index, 1);
    this.names.set(current);
    this.persist();
  }

  completeAddition(): void {
    if (this.submitting() || this.waiting()) return;

    if (this.names().length === 0) {
      this.submitError.set('Debes añadir al menos un nombre');
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    this.apiService
      .addNames(this.listId, { names: this.names() })
      .pipe(switchMap(() => this.apiService.finishAddition(this.listId)))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.localStorage.clearListCache(this.listId);
          this.dashboardService.invalidate();
          this.router.navigate(['/']);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          if (error instanceof HttpErrorResponse && error.status === 401) {
            this.showReAuth.set(true);
            this.reAuthError.set(null);
            return;
          }
          this.submitError.set('Ha habido un error, inténtelo de nuevo');
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
          this.completeAddition();
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

  openInviteModal(): void {
    this.showInviteModal.set(true);
  }

  closeInviteModal(): void {
    this.showInviteModal.set(false);
  }

  private persist(): void {
    this.localStorage.setItem<string[]>(this.listId, 'suggestions', this.names());
  }
}