import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { ApiService } from './api.service';
import { UserProfile } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly apiService = inject(ApiService);
  private tokenSignal = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));
  private profileSignal = signal<UserProfile | null>(null);
  private profileRequested = false;

  readonly isAdmin = computed(() => this.profileSignal()?.role === 'ADMIN');

  getToken(): string | null {
    return this.tokenSignal();
  }

  getProfile(): UserProfile | null {
    return this.profileSignal();
  }

  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.tokenSignal.set(token);
  }

  loadProfile(): Observable<UserProfile | null> {
    if (this.profileRequested) {
      return of(this.profileSignal());
    }
    this.profileRequested = true;
    return this.apiService.getMe().pipe(
      map((profile) => {
        this.profileSignal.set(profile);
        return profile;
      }),
      catchError(() => {
        this.profileRequested = false;
        this.profileSignal.set(null);
        return of(null);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.tokenSignal.set(null);
    this.profileSignal.set(null);
    this.profileRequested = false;
  }
}
