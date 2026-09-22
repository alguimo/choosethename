import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api.service';
import { ListResponse } from '../models/api.models';

const CACHE_TTL_MS = 5 * 60 * 1000;

export type TimeProvider = () => number;

export const NOW_PROVIDER = new InjectionToken<TimeProvider>('NOW_PROVIDER', {
  providedIn: 'root',
  factory: () => () => Date.now(),
});

interface CachedLists {
  lists: ListResponse[];
  fetchedAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly api = inject(ApiService);
  private readonly now = inject(NOW_PROVIDER);
  private cache: CachedLists | null = null;

  getMyLists(): Observable<ListResponse[]> {
    if (this.isFresh()) {
      return of(this.cache!.lists);
    }

    return this.api.getMyLists().pipe(
      map((lists) => {
        this.cache = { lists, fetchedAt: this.now() };
        return lists;
      }),
    );
  }

  invalidate(): void {
    this.cache = null;
  }

  private isFresh(): boolean {
    return !!this.cache && this.now() - this.cache.fetchedAt < CACHE_TTL_MS;
  }
}
