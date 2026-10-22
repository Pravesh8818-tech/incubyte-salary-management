import { computed, Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class WriteTokenService {
  private readonly tokenValue = signal<string | null>(null);
  readonly tokenConfigured = computed(() => Boolean(this.tokenValue()));

  getToken(): string | null {
    return this.tokenValue();
  }

  setToken(token: string): void {
    this.tokenValue.set(token.trim() || null);
  }

  clearToken(): void {
    this.tokenValue.set(null);
  }
}
