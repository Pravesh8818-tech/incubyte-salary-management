import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Employee, EmployeePage, EmployeeQuery, EmployeeService, SalaryAnalytics, SalaryAnalyticsFilterOptions } from './services/employee';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
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

  // Regular variable for search query (two-way binding with ngModel)
  protected searchQuery: string = '';
  protected selectedCurrency = '';
  protected countryFilter = '';
  protected departmentFilter = '';
  private employeeRequestId = 0;
  private analyticsRequestId = 0;
  private filterOptionsRequestId = 0;

  constructor(private employeeService: EmployeeService) {}

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
          this.selectedCurrency = currencies.includes('USD') ? 'USD' : currencies[0];
          this.loadSalaryAnalytics();
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
}
