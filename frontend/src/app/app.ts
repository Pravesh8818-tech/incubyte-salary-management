import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Employee, EmployeePage, EmployeeService } from './services/employee';

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

  // Regular variable for search query (two-way binding with ngModel)
  protected searchQuery: string = '';

  constructor(private employeeService: EmployeeService) {}

  // Lifecycle hook
  ngOnInit(): void {
    this.loadEmployees();
  }

  // Load employees based on current state
  loadEmployees(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const query: any = {
      page: this.pageIndex(),
      size: this.pageSize(),
      search: this.searchQuery.trim() || undefined,
    };

    // Note: We are not implementing sort, country, department, minSalary, maxSalary for simplicity
    // as per the task, we only need search and pagination.

    this.employeeService.getEmployees(query).subscribe({
      next: (page: EmployeePage) => {
        this.employees.set(page.content);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        // Note: the page number from the API is zero-based? The Spring Data Page number is zero-based.
        // We'll keep our pageIndex as zero-based to match.
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading employees:', err);
        this.errorMessage.set('Failed to load employees. Please try again.');
        this.loading.set(false);
      },
    });
  }

  // Search employees (triggered by enter key or search button)
  searchEmployees(): void {
    this.pageIndex.set(0); // reset to first page on search
    this.loadEmployees();
  }

  // Pagination methods
  loadPreviousPage(): void {
    if (this.hasPreviousPage()) {
      this.pageIndex.update((index) => index - 1);
      this.loadEmployees();
    }
  }

  loadNextPage(): void {
    if (this.hasNextPage()) {
      this.pageIndex.update((index) => index + 1);
      this.loadEmployees();
    }
  }

  // Helper methods to check if we can go to next/previous page
  hasPreviousPage(): boolean {
    return this.pageIndex() > 0;
  }

  hasNextPage(): boolean {
    return this.pageIndex() < this.totalPages() - 1;
  }
}