import { Injectable, InjectionToken, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiService } from './api.service';
import { ListResponse } from '../models/api.models';

const CACHE_TTL_MS = 5 * 60 * 1000;

export type TimeProvider = () => number;

export const NOW_PROVIDER = new InjectionToken<TimeProvider>('NOW_PROVIDER', {
  providedIn: 'root',
  factory: () => () => Date.now(),
});

interface CachedList {
  list: ListResponse | null;
  fetchedAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly api = inject(ApiService);
  private readonly now = inject(NOW_PROVIDER);
  private cache: CachedList | null = null;

  getActiveList(): Observable<ListResponse | null> {
    if (this.isFresh()) {
      return of(this.cache!.list);
    }

    return this.api.getActiveList().pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(null);
        }
        return throwError(() => error);
      }),
      map((list) => {
        this.cache = { list, fetchedAt: this.now() };
        return list;
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