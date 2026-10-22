import { InjectionToken } from '@angular/core';

export interface SalaryManagementRuntimeConfig {
  apiBaseUrl?: string;
}

type ConfiguredGlobal = typeof globalThis & {
  salaryManagementConfig?: SalaryManagementRuntimeConfig;
};

const LOCAL_API_BASE_URL = 'http://localhost:8080/api';

export function resolveApiBaseUrl(config: SalaryManagementRuntimeConfig | undefined =
  (globalThis as ConfiguredGlobal).salaryManagementConfig): string {
  const configuredUrl = config?.apiBaseUrl?.trim();
  const baseUrl = configuredUrl || LOCAL_API_BASE_URL;
  return baseUrl.replace(/\/+$/, '');
}

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => resolveApiBaseUrl(),
});
