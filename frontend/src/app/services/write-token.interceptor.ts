import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_BASE_URL } from './api-config';
import { WriteTokenService } from './write-token';

const WRITE_METHODS = new Set(['POST', 'PUT', 'DELETE']);

export const writeTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (!WRITE_METHODS.has(request.method) || request.headers.has('Authorization')) {
    return next(request);
  }

  const token = inject(WriteTokenService).getToken();
  if (!token) {
    return next(request);
  }

  const configuredBase = new URL(inject(API_BASE_URL));
  const requestUrl = new URL(request.url, configuredBase);
  const basePath = configuredBase.pathname.replace(/\/+$/, '');
  const requestPath = requestUrl.pathname.replace(/\/+$/, '');
  const matchesApiPath = requestPath === basePath || requestPath.startsWith(`${basePath}/`);

  if (requestUrl.origin !== configuredBase.origin || !matchesApiPath) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
