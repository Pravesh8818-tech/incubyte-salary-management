# SalaryManagementUi

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.27.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## API URL and employee write access

The frontend reads its public API base URL from `public/app-config.js`. The local default is `http://localhost:8080/api`, which matches the backend's local port and employee/analytics route prefix. For a deployment, configure `apiBaseUrl` to the backend's public HTTPS URL including `/api` before building or publishing the frontend. This is public configuration; do not put credentials or the write token in this file.

Employee list/by-ID reads and salary analytics are public. Create, update, and delete requests require the backend's `APP_WRITE_API_TOKEN` environment variable. If it is unset, the backend rejects writes with HTTP 401. The operator enters the same token in the masked **Write API access** field in the UI. The token is held in memory, is cleared on reload, and can be cleared with the UI button. The interceptor attaches it only to matching POST, PUT, and DELETE requests for the configured API origin and path; it never adds the token to GET requests. CORS restricts browser origins and does not replace API authorization. This shared token does not provide individual user accounts or roles; public deployments must contain only synthetic, non-sensitive employee data.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## End-to-end testing

No end-to-end test runner or `e2e` target is configured in this project. The available automated frontend tests are unit/component tests run with `ng test`; use the root README for the complete assessment verification commands.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
