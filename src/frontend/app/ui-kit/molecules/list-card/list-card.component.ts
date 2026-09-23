import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

import { UiBadgeComponent } from '../../atoms/badge/badge.component';
import { UiButtonComponent } from '../../atoms/button/button.component';
import { phaseBadgeColor, phaseLabel } from '../../phase-presentation';

@Component({
  selector: 'ui-list-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiBadgeComponent, UiButtonComponent, MatIcon],
  template: `
    <div
      class="ui-list-card"
      role="button"
      tabindex="0"
      [attr.aria-label]="'Abrir lista ' + title()"
      (click)="onActivate()"
      (keydown.enter)="onActivate()"
      (keydown.space)="onActivate($event)"
    >
      <div class="ui-list-card__heading">
        <h3 class="ui-list-card__title">{{ title() }}</h3>
        <ui-badge [label]="badgeLabel()" [color]="badgeColor()"></ui-badge>
      </div>
      <div class="ui-list-card__members">
        <mat-icon class="ui-list-card__members-icon">people</mat-icon>
        <span>{{ memberCountLabel() }}</span>
      </div>
      @if (showInvite()) {
        <div
          class="ui-list-card__invite"
          role="button"
          tabindex="0"
          (click)="onInvite($event)"
          (keydown.enter)="onInvite($event)"
          (keydown.space)="onInvite($event)"
        >
          <ui-button label="Invitar" variant="ghost"></ui-button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .ui-list-card {
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-md);
        padding: var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline-variant);
        border-radius: var(--ui-radius-md);
        background-color: var(--ui-color-surface);
        box-shadow: var(--ui-shadow-sm);
        cursor: pointer;
        transition:
          box-shadow var(--ui-transition-fast),
          border-color var(--ui-transition-fast);
      }

      .ui-list-card:hover,
      .ui-list-card:focus-visible {
        border-color: var(--ui-color-primary);
        box-shadow: var(--ui-shadow-md);
      }

      .ui-list-card:focus-visible {
        outline: 2px solid var(--ui-color-focus-ring);
        outline-offset: 2px;
      }

      .ui-list-card__heading {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: var(--ui-spacing-sm);
      }

      .ui-list-card__title {
        margin: 0;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        font-weight: var(--ui-font-weight-semibold);
        line-height: var(--ui-line-height-tight);
        color: var(--ui-color-on-surface);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .ui-list-card__members {
        display: flex;
        align-items: center;
        gap: var(--ui-spacing-xs);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface-variant);
      }

      .ui-list-card__members-icon {
        width: 16px;
        height: 16px;
        font-size: 16px;
        line-height: 16px;
      }

      .ui-list-card__invite {
        display: inline-block;
        align-self: flex-start;
      }
    `,
  ],
})
export class UiListCardComponent {
  readonly title = input('');
  readonly phase = input('');
  readonly memberCount = input<number>(0);
  readonly invitationCode = input('');
  readonly invitationsOpen = input<boolean>(false);
  readonly clicked = output<void>();
  readonly inviteClicked = output<string>();

  readonly badgeLabel = computed(() => phaseLabel(this.phase()));
  readonly badgeColor = computed(() => phaseBadgeColor(this.phase()));
  readonly memberCountLabel = computed(() =>
    this.memberCount() === 1 ? '1 miembro' : `${this.memberCount()} miembros`,
  );
  readonly showInvite = computed(
    () => this.invitationsOpen() && this.invitationCode().length > 0,
  );

  onActivate(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.clicked.emit();
  }

  onInvite(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.inviteClicked.emit(this.invitationCode());
  }
}