// Public runtime configuration. Set apiBaseUrl to the deployed backend's public HTTPS API URL.
// This file is intentionally separate from the Angular bundles and must never contain secrets.
globalThis.salaryManagementConfig = {
  apiBaseUrl: 'http://localhost:8080/api',
};
