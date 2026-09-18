import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { By } from '@angular/platform-browser';

import { UiDraggableRankingListComponent } from '@app/ui-kit/organisms/draggable-ranking-list/draggable-ranking-list.component';

function buildDropEvent(
  dropList: CdkDropList,
  previousIndex: number,
  currentIndex: number,
): CdkDragDrop<string[]> {
  return {
    previousIndex,
    currentIndex,
    item: undefined as never,
    container: dropList,
    previousContainer: dropList,
    isPointerOverContainer: true,
    distance: { x: 0, y: 0 },
    dropPoint: { x: 0, y: 0 },
    event: null as unknown as DragEvent,
  };
}

describe('UiDraggableRankingListComponent', () => {
  let fixture: ComponentFixture<UiDraggableRankingListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiDraggableRankingListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiDraggableRankingListComponent);
    fixture.detectChanges();
  });

  // TS-13: emits rankingsChanged with the reordered array after a drop (FR-32).
  it('should emit the reordered array after dropping an item lower', () => {
    fixture.componentRef.setInput('items', ['a', 'b', 'c']);
    fixture.detectChanges();

    const spy = jasmine.createSpy('rankingsChanged');
    fixture.componentInstance.rankingsChanged.subscribe(spy);

    const dropList = fixture.debugElement.query(By.directive(CdkDropList))
      .injector.get(CdkDropList);
    dropList.dropped.emit(buildDropEvent(dropList, 0, 2));

    expect(spy).toHaveBeenCalledOnceWith(['b', 'c', 'a']);
  });

  it('should not emit the reordered array when dropping on the same index', () => {
    fixture.componentRef.setInput('items', ['a', 'b', 'c']);
    fixture.detectChanges();

    const spy = jasmine.createSpy('rankingsChanged');
    fixture.componentInstance.rankingsChanged.subscribe(spy);

    const dropList = fixture.debugElement.query(By.directive(CdkDropList))
      .injector.get(CdkDropList);
    dropList.dropped.emit(buildDropEvent(dropList, 1, 1));

    expect(spy).not.toHaveBeenCalled();
  });

  // TS-14: with disabled=true prevents drag interaction (FR-33).
  it('should disable the drop list and ignore drops when disabled', () => {
    fixture.componentRef.setInput('items', ['a', 'b']);
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const dropList = fixture.debugElement.query(By.directive(CdkDropList))
      .injector.get(CdkDropList);
    expect(dropList.disabled).toBeTrue();

    const spy = jasmine.createSpy('rankingsChanged');
    fixture.componentInstance.rankingsChanged.subscribe(spy);

    dropList.dropped.emit(buildDropEvent(dropList, 0, 1));
    expect(spy).not.toHaveBeenCalled();
  });

  it('should keep the visual list intact when disabled', () => {
    fixture.componentRef.setInput('items', ['a', 'b']);
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const texts = fixture.nativeElement.querySelectorAll('.ui-ranking-list__text');

    expect(texts.length).toBe(2);
    expect((texts[0] as HTMLElement).textContent).toBe('a');
    expect((texts[1] as HTMLElement).textContent).toBe('b');

    expect(
      fixture.nativeElement.querySelector('.ui-ranking-list').classList.contains(
        'ui-ranking-list--disabled',
      ),
    ).toBeTrue();
  });

  // Edge case: empty items array renders an empty container with no drag handles.
  it('should render an empty container without drag items when items is empty', () => {
    fixture.componentRef.setInput('items', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[cdkdrag]')).toHaveSize(0);
    expect(fixture.nativeElement.querySelector('.ui-ranking-list__empty')).toBeTruthy();
  });
});