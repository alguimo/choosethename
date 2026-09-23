import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { DashboardService } from '../../services/dashboard.service';
import { ApiError, CreateListRequest, JoinListRequest, ListResponse } from '../../models/api.models';
import { UiButtonComponent } from '../../ui-kit/atoms/button/button.component';
import { UiValidationMessageComponent } from '../../ui-kit/atoms/validation-message/validation-message.component';
import { UiModalComponent } from '../../ui-kit/organisms/modal/modal.component';
import { UiInputFieldComponent } from '../../ui-kit/atoms/input-field/input-field.component';
import { UiListCardComponent } from '../../ui-kit/molecules/list-card/list-card.component';
import { UiInviteModalComponent } from '../../ui-kit/molecules/invite-modal/invite-modal.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    UiButtonComponent,
    UiValidationMessageComponent,
    UiModalComponent,
    UiInputFieldComponent,
    UiListCardComponent,
    UiInviteModalComponent,
  ],
  template: `
    <section class="dashboard">
      <h2 class="dashboard__title">Mis listas</h2>

      <div class="dashboard__actions">
        <ui-button label="Crear lista nueva" (clicked)="openCreateModal()"></ui-button>
        <ui-button
          label="Unirse con código"
          variant="secondary"
          (clicked)="openJoinModal()"
        ></ui-button>
      </div>

      @if (error()) {
        <ui-validation-message type="error" [message]="error()!" />
      }

      @if (loading()) {
        <p class="dashboard__loading">Cargando...</p>
      }

      @if (!loading() && !error() && lists().length === 0) {
        <div class="dashboard__empty">
          <p class="dashboard__empty-text">No tienes ninguna lista</p>
        </div>
      }

      @for (list of lists(); track list.id) {
        <ui-list-card
          [title]="list.name"
          [phase]="list.phase"
          [memberCount]="list.members.length"
          [invitationCode]="list.invitationCode"
          [invitationsOpen]="list.invitationsOpen"
          (clicked)="onCardClick(list)"
          (inviteClicked)="openInviteModal(list)"
        />
      }

      <ui-modal title="Crear lista nueva" [visible]="showCreateModal()" (closed)="closeCreateModal()">
        <form [formGroup]="createForm" (ngSubmit)="createList()">
          <ui-input-field
            label="Nombre de la lista"
            formControlName="name"
            placeholder="Ej. Nombres para el gato"
          ></ui-input-field>
          @if (createError()) {
            <ui-validation-message type="error" [message]="createError()!" />
          }
          <div class="dashboard__modal-actions">
            <ui-button
              label="Cancelar"
              variant="ghost"
              (clicked)="closeCreateModal()"
            ></ui-button>
            <ui-button
              label="Crear lista"
              [disabled]="createForm.invalid || creating()"
              [loading]="creating()"
            ></ui-button>
          </div>
        </form>
      </ui-modal>

      <ui-modal title="Unirse con código" [visible]="showJoinModal()" (closed)="closeJoinModal()">
        <form [formGroup]="joinForm" (ngSubmit)="joinList()">
          <ui-input-field
            label="Código"
            formControlName="code"
            placeholder="ABC123"
            [maxLength]="6"
          ></ui-input-field>
          @if (joinError()) {
            <ui-validation-message type="error" [message]="joinError()!" />
          }
          <div class="dashboard__modal-actions">
            <ui-button
              label="Cancelar"
              variant="ghost"
              (clicked)="closeJoinModal()"
            ></ui-button>
            <ui-button
              label="Unirse"
              [disabled]="joinForm.invalid || joining()"
              [loading]="joining()"
            ></ui-button>
          </div>
        </form>
      </ui-modal>

      <ui-invite-modal
        [title]="'Invitar a ' + (invitationTarget()?.name ?? '')"
        [code]="invitationTarget()?.invitationCode ?? ''"
        [visible]="invitationTarget() !== null"
        (closed)="closeInviteModal()"
      ></ui-invite-modal>
    </section>
  `,
  styles: [
    `
      .dashboard {
        max-width: 480px;
        margin: 2rem auto;
        padding: 1rem;
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
      }

      .dashboard__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-xl);
        font-weight: var(--ui-font-weight-semibold);
        color: var(--ui-color-on-surface);
      }

      .dashboard__loading {
        margin: 0;
        font-family: var(--ui-font-family);
        color: var(--ui-color-on-surface-variant);
      }

      .dashboard__actions {
        display: flex;
        flex-wrap: wrap;
        gap: var(--ui-spacing-sm);
      }

      .dashboard__empty {
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
        padding: var(--ui-spacing-lg);
        border: 1px dashed var(--ui-color-outline);
        border-radius: var(--ui-radius-md);
        text-align: center;
      }

      .dashboard__empty-text {
        margin: 0;
        font-family: var(--ui-font-family);
        color: var(--ui-color-on-surface-variant);
      }

      .dashboard__modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: var(--ui-spacing-sm);
        margin-top: var(--ui-spacing-md);
      }
    `,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly apiService = inject(ApiService);
  private readonly dashboardService = inject(DashboardService);
  private readonly router = inject(Router);

  readonly lists = signal<ListResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showCreateModal = signal(false);
  readonly showJoinModal = signal(false);
  readonly invitationTarget = signal<ListResponse | null>(null);
  readonly createError = signal<string | null>(null);
  readonly joinError = signal<string | null>(null);
  readonly creating = signal(false);
  readonly joining = signal(false);

  readonly createForm = this.fb.group({
    name: ['', Validators.required],
  });

  readonly joinForm = this.fb.group({
    code: ['', [Validators.required, Validators.minLength(6)]],
  });

  ngOnInit(): void {
    this.loadLists();
  }

  loadLists(): void {
    this.loading.set(true);
    this.error.set(null);
    this.dashboardService.getMyLists().subscribe({
      next: (lists) => {
        this.lists.set(lists);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Ha habido un error, inténtelo de nuevo');
      },
    });
  }

  onCardClick(list: ListResponse): void {
    this.router.navigate(['/lists', list.id, this.viewForPhase(list.phase)]);
  }

  openInviteModal(list: ListResponse): void {
    this.invitationTarget.set(list);
  }

  closeInviteModal(): void {
    this.invitationTarget.set(null);
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

  openCreateModal(): void {
    this.createError.set(null);
    this.createForm.reset();
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.createError.set(null);
  }

  createList(): void {
    if (!this.showCreateModal() || this.creating() || this.createForm.invalid) return;

    this.creating.set(true);
    this.createError.set(null);
    const request: CreateListRequest = {
      name: this.createForm.value.name?.trim() ?? '',
    };

    this.apiService.createList(request).subscribe({
      next: (list) => {
        this.creating.set(false);
        this.showCreateModal.set(false);
        this.dashboardService.invalidate();
        this.router.navigate(['/lists', list.id, 'suggestion']);
      },
      error: (error: unknown) => {
        this.creating.set(false);
        this.createError.set(this.extractError(error));
      },
    });
  }

  openJoinModal(): void {
    this.joinError.set(null);
    this.joinForm.reset();
    this.showJoinModal.set(true);
  }

  closeJoinModal(): void {
    this.showJoinModal.set(false);
    this.joinError.set(null);
  }

  joinList(): void {
    if (!this.showJoinModal() || this.joining() || this.joinForm.invalid) return;

    this.joining.set(true);
    this.joinError.set(null);
    const request: JoinListRequest = {
      code: this.joinForm.value.code?.trim() ?? '',
    };

    this.apiService.joinList(request).subscribe({
      next: (list) => {
        this.joining.set(false);
        this.showJoinModal.set(false);
        this.dashboardService.invalidate();
        this.router.navigate(['/lists', list.id, 'suggestion']);
      },
      error: (error: unknown) => {
        this.joining.set(false);
        this.joinError.set(this.extractError(error));
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
