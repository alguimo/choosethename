import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {
  private getPrefix(listId: string): string {
    return `list_${listId}_`;
  }

  setItem<T>(listId: string, key: string, value: T): void {
    localStorage.setItem(`${this.getPrefix(listId)}${key}`, JSON.stringify(value));
  }

  getItem<T>(listId: string, key: string): T | null {
    const data = localStorage.getItem(`${this.getPrefix(listId)}${key}`);
    return data ? JSON.parse(data) : null;
  }

  removeItem(listId: string, key: string): void {
    localStorage.removeItem(`${this.getPrefix(listId)}${key}`);
  }

  clearListCache(listId: string): void {
    const prefix = this.getPrefix(listId);
    Object.keys(localStorage).forEach(key => {
      if (key.startsWith(prefix)) {
        localStorage.removeItem(key);
      }
    });
  }
}
