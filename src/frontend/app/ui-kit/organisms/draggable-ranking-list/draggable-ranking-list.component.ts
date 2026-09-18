import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'ui-draggable-ranking-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DragDropModule, MatIcon],
  template: `
    <div
      class="ui-ranking-list"
      [class.ui-ranking-list--disabled]="disabled()"
      cdkDropList
      [cdkDropListDisabled]="disabled()"
      (cdkDropListDropped)="onDrop($event)"
    >
      @for (item of items(); track $index) {
        <div class="ui-ranking-list__item" cdkDrag>
          <mat-icon class="ui-ranking-list__drag-handle" cdkDragHandle>drag_indicator</mat-icon>
          <span class="ui-ranking-list__text">{{ item }}</span>
        </div>
      } @empty {
        <p class="ui-ranking-list__empty">No hay elementos para ordenar</p>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .ui-ranking-list {
        display: flex;
        flex-direction: column;
        gap: var(--ui-spacing-sm);
        border-radius: var(--ui-radius-md);
        padding: var(--ui-spacing-sm);
        transition: background-color var(--ui-transition-fast);
      }

      .ui-ranking-list__item {
        display: flex;
        align-items: center;
        gap: var(--ui-spacing-sm);
        padding: var(--ui-spacing-sm) var(--ui-spacing-md);
        border: 1px solid var(--ui-color-outline);
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-surface);
        color: var(--ui-color-on-surface);
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-base);
        cursor: grab;
      }

      .ui-ranking-list__item:active {
        cursor: grabbing;
      }

      .ui-ranking-list__drag-handle {
        width: 20px;
        height: 20px;
        font-size: 20px;
        line-height: 20px;
        color: var(--ui-color-on-surface-variant);
        flex-shrink: 0;
      }

      .ui-ranking-list__text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .ui-ranking-list__empty {
        margin: 0;
        padding: var(--ui-spacing-md);
        text-align: center;
        font-family: var(--ui-font-family);
        font-size: var(--ui-font-size-sm);
        color: var(--ui-color-on-surface-variant);
      }

      .ui-ranking-list--disabled .ui-ranking-list__item {
        cursor: default;
        opacity: 0.6;
      }

      .ui-ranking-list .cdk-drop-list-dragging .ui-ranking-list__item:not(.cdk-drag-placeholder) {
        background-color: var(--ui-color-primary-container);
      }

      .ui-ranking-list .cdk-drag-preview {
        border-radius: var(--ui-radius-sm);
        background-color: var(--ui-color-surface);
        box-shadow: var(--ui-shadow-lg);
        opacity: 0.9;
      }

      .ui-ranking-list .cdk-drag-placeholder {
        border: 1px dashed var(--ui-color-primary);
        background-color: var(--ui-color-primary-container);
        opacity: 0.5;
      }
    `,
  ],
})
export class UiDraggableRankingListComponent {
  readonly items = input<string[]>([]);
  readonly disabled = input<boolean>(false);
  readonly rankingsChanged = output<string[]>();

  onDrop(event: CdkDragDrop<string[]>): void {
    if (this.disabled()) {
      return;
    }

    const reordered = [...this.items()];
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(reordered, event.previousIndex, event.currentIndex);
    this.rankingsChanged.emit(reordered);
  }
}