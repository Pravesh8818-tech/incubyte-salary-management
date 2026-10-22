import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Employee, EmployeePage, EmployeeQuery, EmployeeService, EmployeeWriteRequest, SalaryAnalytics, SalaryAnalyticsFilterOptions } from './services/employee';
import { Observable } from 'rxjs';
import { WriteTokenService } from './services/write-token';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('salary-management-ui');

  // Signals for state
  protected readonly loading = signal<boolean>(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly employees = signal<Employee[]>([]);
  protected readonly pageIndex = signal<number>(0);
  protected readonly pageSize = signal<number>(10);
  protected readonly totalPages = signal<number>(0);
  protected readonly totalElements = signal<number>(0);
  protected readonly isFirstPage = signal<boolean>(true);
  protected readonly isLastPage = signal<boolean>(true);
  protected readonly failedPageIndex = signal<number | null>(null);
  protected readonly salaryAnalytics = signal<SalaryAnalytics | null>(null);
  protected readonly analyticsLoading = signal<boolean>(false);
  protected readonly analyticsError = signal<string | null>(null);
  protected readonly salaryCurrencies = signal<string[]>([]);
  protected readonly currenciesLoading = signal<boolean>(false);
  protected readonly salaryFilterOptions = signal<SalaryAnalyticsFilterOptions>({ countries: [], departments: [] });
  protected readonly filterOptionsLoading = signal<boolean>(false);
  protected readonly filterOptionsError = signal<string | null>(null);
  protected readonly employeeFormVisible = signal(false);
  protected readonly editingEmployeeId = signal<number | null>(null);
  protected readonly editLoading = signal(false);
  protected readonly mutationLoading = signal(false);
  protected readonly mutationMessage = signal<string | null>(null);
  protected readonly mutationError = signal<string | null>(null);
  protected readonly employeeFormError = signal<string | null>(null);
  protected readonly employeeFieldErrors = signal<Record<string, string>>({});
  protected readonly writeTokenEntered = signal(false);
  protected writeTokenDraft = '';

  private readonly formBuilder = inject(FormBuilder);
  private readonly writeTokenService = inject(WriteTokenService);
  readonly employeeForm = this.formBuilder.group({
    employeeCode: ['', Validators.required],
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    country: ['', Validators.required],
    department: ['', Validators.required],
    jobTitle: ['', Validators.required],
    currency: ['USD', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
    annualSalary: [null as number | null, [
      Validators.required,
      Validators.min(0),
      Validators.max(99999999.99),
      Validators.pattern(/^\d+(\.\d{1,2})?$/),
    ]],
  });

  // Regular variable for search query (two-way binding with ngModel)
  protected searchQuery: string = '';
  protected selectedCurrency = '';
  protected countryFilter = '';
  protected departmentFilter = '';
  private employeeRequestId = 0;
  private analyticsRequestId = 0;
  private filterOptionsRequestId = 0;
  private editRequestId = 0;

  constructor(private employeeService: EmployeeService) {}

  saveWriteToken(): void {
    this.writeTokenService.setToken(this.writeTokenDraft);
    this.writeTokenDraft = '';
    this.writeTokenEntered.set(this.writeTokenService.tokenConfigured());
  }

  clearWriteToken(): void {
    this.writeTokenService.clearToken();
    this.writeTokenDraft = '';
    this.writeTokenEntered.set(false);
  }

  // Lifecycle hook
  ngOnInit(): void {
    this.loadEmployees();
    this.loadSalaryCurrencies();
    this.loadSalaryFilterOptions();
  }

  loadSalaryFilterOptions(): void {
    const requestId = ++this.filterOptionsRequestId;
    this.filterOptionsLoading.set(true);
    this.filterOptionsError.set(null);
    this.employeeService.getSalaryFilterOptions().subscribe({
      next: (options) => {
        if (requestId !== this.filterOptionsRequestId) return;
        this.salaryFilterOptions.set(options);
        this.filterOptionsLoading.set(false);
      },
      error: () => {
        if (requestId !== this.filterOptionsRequestId) return;
        this.filterOptionsError.set('Failed to load country and department options. Please try again.');
        this.filterOptionsLoading.set(false);
      },
    });
  }

  loadSalaryCurrencies(): void {
    this.currenciesLoading.set(true);
    this.employeeService.getSalaryCurrencies().subscribe({
      next: (currencies) => {
        this.currenciesLoading.set(false);
        this.salaryCurrencies.set(currencies);
        if (currencies.length > 0) {
          if (!currencies.includes(this.selectedCurrency)) {
            this.selectedCurrency = currencies.includes('USD') ? 'USD' : currencies[0];
          }
          this.loadSalaryAnalytics();
        } else {
          this.selectedCurrency = '';
          this.salaryAnalytics.set(null);
        }
      },
      error: () => {
        this.currenciesLoading.set(false);
        this.analyticsError.set('Failed to load salary currencies. Please try again.');
      },
    });
  }

  loadSalaryAnalytics(): void {
    if (!this.selectedCurrency) return;
    const requestId = ++this.analyticsRequestId;
    this.analyticsLoading.set(true);
    this.analyticsError.set(null);
    this.salaryAnalytics.set(null);
    this.employeeService.getSalaryAnalytics(this.selectedCurrency, this.countryFilter, this.departmentFilter).subscribe({
      next: (analytics) => {
        if (requestId !== this.analyticsRequestId) return;
        this.salaryAnalytics.set(analytics);
        this.analyticsLoading.set(false);
      },
      error: () => {
        if (requestId !== this.analyticsRequestId) return;
        this.analyticsError.set('Failed to load salary analytics. Please try again.');
        this.analyticsLoading.set(false);
      },
    });
  }

  // Load employees based on current state
  loadEmployees(requestedPage: number = this.pageIndex()): void {
    const requestId = ++this.employeeRequestId;
    this.loading.set(true);
    this.errorMessage.set(null);
    this.employees.set([]);

    const query: EmployeeQuery = {
      page: Math.max(0, requestedPage),
      size: this.pageSize(),
      search: this.searchQuery.trim() || undefined,
    };

    // Note: We are not implementing sort, country, department, minSalary, maxSalary for simplicity
    // as per the task, we only need search and pagination.

    this.employeeService.getEmployees(query).subscribe({
      next: (page: EmployeePage) => {
        if (requestId !== this.employeeRequestId) return;
        if (query.page > 0 && query.page >= page.totalPages) {
          this.loadEmployees(Math.max(0, page.totalPages - 1));
          return;
        }
        this.employees.set(page.content);
        this.pageIndex.set(page.number);
        this.pageSize.set(page.size);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.isFirstPage.set(page.first);
        this.isLastPage.set(page.last);
        this.failedPageIndex.set(null);
        // Note: the page number from the API is zero-based? The Spring Data Page number is zero-based.
        // We'll keep our pageIndex as zero-based to match.
        this.loading.set(false);
      },
      error: (error) => {
        if (requestId !== this.employeeRequestId) return;
        this.employees.set([]);
        this.failedPageIndex.set(query.page);
        this.errorMessage.set(this.employeeLoadError(error, query.page));
        this.loading.set(false);
      },
    });
  }

  retryEmployees(): void {
    this.loadEmployees(this.failedPageIndex() ?? this.pageIndex());
  }

  private employeeLoadError(error: unknown, requestedPage: number): string {
    const displayPage = requestedPage + 1;
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return `Could not reach the employee API while loading page ${displayPage}. Check the backend connection and browser access.`;
    }
    const status = error instanceof HttpErrorResponse ? ` (HTTP ${error.status})` : '';
    return `Could not load page ${displayPage}${status}. Please try again.`;
  }

  // Search employees (triggered by enter key or search button)
  searchEmployees(): void {
    this.loadEmployees(0);
  }

  // Pagination methods
  loadPreviousPage(): void {
    if (!this.loading() && this.hasPreviousPage()) {
      this.loadEmployees(this.pageIndex() - 1);
    }
  }

  loadNextPage(): void {
    if (!this.loading() && this.hasNextPage()) {
      this.loadEmployees(this.pageIndex() + 1);
    }
  }

  // Helper methods to check if we can go to next/previous page
  hasPreviousPage(): boolean {
    return !this.isFirstPage() && this.pageIndex() > 0;
  }

  hasNextPage(): boolean {
    return !this.isLastPage() && this.totalPages() > 0 && this.pageIndex() < this.totalPages() - 1;
  }

  firstVisibleEmployee(): number {
    return this.totalElements() === 0 ? 0 : this.pageIndex() * this.pageSize() + 1;
  }

  lastVisibleEmployee(): number {
    return Math.min((this.pageIndex() + 1) * this.pageSize(), this.totalElements());
  }

  paginationSummary(): string {
    if (this.totalElements() === 0) {
      return 'Page 0 of 0 · Showing 0–0 of 0';
    }
    if (this.employees().length === 0) {
      return `Page ${this.pageIndex() + 1} of ${this.totalPages()} · ${this.totalElements()} employees`;
    }
    return `Page ${this.pageIndex() + 1} of ${this.totalPages()} · Showing ${this.firstVisibleEmployee()}–${this.lastVisibleEmployee()} of ${this.totalElements()}`;
  }

  openCreateForm(): void {
    ++this.editRequestId;
    this.editLoading.set(false);
    this.resetMutationMessages();
    this.editingEmployeeId.set(null);
    this.employeeForm.reset({
      employeeCode: '', fullName: '', email: '', country: '', department: '', jobTitle: '',
      currency: this.selectedCurrency || 'USD', annualSalary: null,
    });
    this.employeeFormVisible.set(true);
  }

  editEmployee(employee: Employee): void {
    this.resetMutationMessages();
    const requestId = ++this.editRequestId;
    this.editingEmployeeId.set(employee.id);
    this.employeeFormVisible.set(true);
    this.editLoading.set(true);
    this.employeeFormError.set(null);
    this.employeeService.getEmployeeById(employee.id).subscribe({
      next: (loaded) => {
        if (requestId !== this.editRequestId) return;
        this.employeeForm.patchValue({
          employeeCode: loaded.employeeCode,
          fullName: loaded.fullName,
          email: loaded.email,
          country: loaded.country,
          department: loaded.department,
          jobTitle: loaded.jobTitle,
          currency: loaded.currency,
          annualSalary: loaded.annualSalary,
        });
        this.editLoading.set(false);
      },
      error: (error) => {
        if (requestId !== this.editRequestId) return;
        this.editLoading.set(false);
        this.employeeFormError.set(this.apiErrorMessage(error, 'Could not load employee details.'));
      },
    });
  }

  closeEmployeeForm(): void {
    ++this.editRequestId;
    this.employeeFormVisible.set(false);
    this.editLoading.set(false);
    this.mutationLoading.set(false);
    this.editingEmployeeId.set(null);
    this.employeeFormError.set(null);
  }

  saveEmployee(): void {
    if (this.mutationLoading()) return;
    this.employeeFieldErrors.set({});
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      return;
    }
    const value = this.employeeForm.getRawValue();
    const request: EmployeeWriteRequest = {
      employeeCode: value.employeeCode!.trim(),
      fullName: value.fullName!.trim(),
      email: value.email!.trim(),
      country: value.country!.trim(),
      department: value.department!.trim(),
      jobTitle: value.jobTitle!.trim(),
      currency: value.currency!.trim().toUpperCase(),
      annualSalary: Number(value.annualSalary),
    };
    const id = this.editingEmployeeId();
    const operation: Observable<Employee> = id === null
      ? this.employeeService.createEmployee(request)
      : this.employeeService.updateEmployee(id, request);
    this.mutationLoading.set(true);
    this.employeeFormError.set(null);
    operation.subscribe({
      next: () => {
        this.mutationLoading.set(false);
        this.employeeFormVisible.set(false);
        this.editingEmployeeId.set(null);
        this.mutationMessage.set(id === null ? 'Employee created successfully.' : 'Employee updated successfully.');
        this.refreshAfterMutation();
      },
      error: (error) => {
        this.mutationLoading.set(false);
        this.applyMutationError(error);
      },
    });
  }

  deleteEmployee(employee: Employee): void {
    if (this.mutationLoading()) return;
    const confirmed = window.confirm(`Delete ${employee.fullName} (${employee.employeeCode})? This action cannot be undone.`);
    if (!confirmed) return;
    this.resetMutationMessages();
    this.mutationLoading.set(true);
    this.employeeService.deleteEmployee(employee.id).subscribe({
      next: () => {
        this.mutationLoading.set(false);
        this.mutationMessage.set(`${employee.fullName} was deleted.`);
        this.refreshAfterMutation();
      },
      error: (error) => {
        this.mutationLoading.set(false);
        this.mutationError.set(this.apiErrorMessage(error, 'Could not delete the employee.'));
      },
    });
  }

  isFieldInvalid(field: keyof typeof this.employeeForm.controls): boolean {
    const control = this.employeeForm.controls[field];
    return (control.invalid && (control.dirty || control.touched)) || !!this.employeeFieldErrors()[field];
  }

  employeeFieldError(field: string, fallback: string): string {
    return this.employeeFieldErrors()[field] ?? fallback;
  }

  clearEmployeeFieldError(field: string): void {
    const errors = { ...this.employeeFieldErrors() };
    delete errors[field];
    this.employeeFieldErrors.set(errors);
  }

  private refreshAfterMutation(): void {
    this.loadEmployees(this.pageIndex());
    this.loadSalaryCurrencies();
  }

  private resetMutationMessages(): void {
    this.mutationMessage.set(null);
    this.mutationError.set(null);
    this.employeeFormError.set(null);
    this.employeeFieldErrors.set({});
  }

  private applyMutationError(error: unknown): void {
    const body = error instanceof HttpErrorResponse ? error.error : null;
    const fields = body && typeof body === 'object' ? body.fields as Record<string, string> | undefined : undefined;
    if (fields) {
      this.employeeFieldErrors.set(fields);
    }
    this.employeeFormError.set(this.apiErrorMessage(error, 'Could not save the employee.'));
  }

  private apiErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'The write API rejected this request. Enter or check the write token, then try again.';
    }
    if (error instanceof HttpErrorResponse && error.error && typeof error.error.message === 'string') {
      return error.error.message;
    }
    return fallback;
  }
}
