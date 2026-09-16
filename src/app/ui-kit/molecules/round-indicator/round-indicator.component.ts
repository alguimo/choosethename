import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'ui-round-indicator',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ui-round-indicator">
      <div class="ui-round-indicator__text" role="status">Ronda {{ currentRound() }} de {{ totalRounds() }}</div>
      <div
        class="ui-round-indicator__track"
        role="presentation"
        [attr.aria-hidden]="true"
      >
        <div class="ui-round-indicator__fill" [style.width.%]="progress()"></div>
      </div>
    </div>
  `,
  styles: [
    `
      .ui-round-indicator__text {
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        font-weight: var(--ui-font-weight-medium);
        color: var(--ui-color-on-surface);
      }

      .ui-round-indicator__track {
        margin-top: var(--ui-spacing-xs);
        height: 6px;
        border-radius: var(--ui-radius-full);
        background-color: var(--ui-color-surface-variant);
        overflow: hidden;
      }

      .ui-round-indicator__fill {
        height: 100%;
        border-radius: var(--ui-radius-full);
        background-color: var(--ui-color-primary);
        transition: width var(--ui-transition-base);
      }
    `,
  ],
})
export class UiRoundIndicatorComponent {
  readonly currentRound = input<number>(1);
  readonly totalRounds = input<number>(1);

  readonly progress = computed(() => {
    const total = this.totalRounds();
    if (total <= 0) {
      return 0;
    }
    return Math.min(100, Math.max(0, (this.currentRound() / total) * 100));
  });
}