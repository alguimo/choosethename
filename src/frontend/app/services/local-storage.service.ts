import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {
  private static readonly LIST_PREFIX = 'list_';

  readonly isAvailable = signal(this.probeAvailability());

  setItem<T>(listId: string, key: string, value: T): void {
    if (!this.isAvailable()) return;
    try {
      localStorage.setItem(`${this.getPrefix(listId)}${key}`, JSON.stringify(value));
    } catch {
      this.isAvailable.set(false);
    }
  }

  getItem<T>(listId: string, key: string): T | null {
    if (!this.isAvailable()) return null;
    try {
      const data = localStorage.getItem(`${this.getPrefix(listId)}${key}`);
      return data ? JSON.parse(data) : null;
    } catch {
      this.isAvailable.set(false);
      return null;
    }
  }

  removeItem(listId: string, key: string): void {
    if (!this.isAvailable()) return;
    try {
      localStorage.removeItem(`${this.getPrefix(listId)}${key}`);
    } catch {
      this.isAvailable.set(false);
    }
  }

  clearListCache(listId: string): void {
    if (!this.isAvailable()) return;
    try {
      const prefix = this.getPrefix(listId);
      Object.keys(localStorage).forEach(key => {
        if (key.startsWith(prefix)) {
          localStorage.removeItem(key);
        }
      });
    } catch {
      this.isAvailable.set(false);
    }
  }

  clearAllListCaches(): void {
    if (!this.isAvailable()) return;
    try {
      Object.keys(localStorage).forEach(key => {
        if (key.startsWith(LocalStorageService.LIST_PREFIX)) {
          localStorage.removeItem(key);
        }
      });
    } catch {
      this.isAvailable.set(false);
    }
  }

  private getPrefix(listId: string): string {
    return `${LocalStorageService.LIST_PREFIX}${listId}_`;
  }

  private probeAvailability(): boolean {
    try {
      const probeKey = 'list_probe_availability';
      localStorage.setItem(probeKey, '1');
      localStorage.removeItem(probeKey);
      return true;
    } catch {
      return false;
    }
  }
}
