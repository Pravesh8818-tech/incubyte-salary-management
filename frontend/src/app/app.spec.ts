import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, Subject } from 'rxjs';
import { App } from './app';
import { Employee, EmployeePage, EmployeeQuery, EmployeeService, SalaryAnalytics, SalaryAnalyticsFilterOptions } from './services/employee';

function employee(code: string): Employee {
  return {
    id: Number(code.slice(3)), employeeCode: code, fullName: `Employee ${code}`,
    email: `${code}@example.test`, country: 'India', department: 'Engineering',
    jobTitle: 'Engineer', currency: 'INR', annualSalary: 50000,
  };
}

function page(number: number, employees = Array.from({ length: number === 2 ? 1 : 10 }, (_, index) =>
  employee(`EMP${String(number * 10 + index + 1).padStart(5, '0')}`))): EmployeePage {
  return {
    content: employees,
    totalElements: 21,
    totalPages: 3,
    number,
    size: 10,
    first: number === 0,
    last: number === 2,
    empty: employees.length === 0,
  };
}

function analyticsResult(
  currency = 'INR',
  countries = ['Canada', 'India'],
  departments = ['Engineering', 'Sales'],
  employeeCount = 3,
): SalaryAnalytics {
  const groups = (names: string[]) => names.map((name) => ({
    name, employeeCount: 1, totalAnnualSalary: 100, averageAnnualSalary: 100,
  }));
  return {
    currency,
    employeeCount,
    totalAnnualSalary: employeeCount * 100,
    averageAnnualSalary: employeeCount === 0 ? 0 : 100,
    byCountry: groups(countries),
    byDepartment: groups(departments),
  };
}

describe('App employee pagination', () => {
  let getEmployees: ReturnType<typeof vi.fn>;
  let getSalaryAnalytics: ReturnType<typeof vi.fn>;
  let getSalaryFilterOptions: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<App>;

  async function createApp(
    employeeRequest: (query: EmployeeQuery) => Observable<EmployeePage>,
    analyticsRequest: (currency: string, country?: string, department?: string) => Observable<SalaryAnalytics> = () => of(analyticsResult()),
    currencies: string[] = [],
    filterOptionsRequest: () => Observable<SalaryAnalyticsFilterOptions> = () => of({
      countries: ['Canada', 'India'], departments: ['Engineering', 'Sales'],
    }),
  ) {
    getEmployees = vi.fn(employeeRequest);
    getSalaryAnalytics = vi.fn(analyticsRequest);
    getSalaryFilterOptions = vi.fn(filterOptionsRequest);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{
        provide: EmployeeService,
        useValue: {
          getEmployees,
          getSalaryCurrencies: vi.fn(() => of(currencies)),
          getSalaryFilterOptions,
          getSalaryAnalytics,
        },
      }],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('loads zero-based pages at the configured page size and displays response metadata', async () => {
    await createApp((query) => of(page(query.page)));
    const app = fixture.componentInstance;

    expect(getEmployees).toHaveBeenCalledWith({ page: 0, size: 10, search: undefined });
    expect(fixture.nativeElement.textContent).toContain('Page 1 of 3 · Showing 1–10 of 21');
    expect(fixture.nativeElement.textContent).toContain('EMP00001');

    app.loadNextPage();
    fixture.detectChanges();
    expect(getEmployees).toHaveBeenLastCalledWith({ page: 1, size: 10, search: undefined });
    expect(fixture.nativeElement.textContent).toContain('Page 2 of 3 · Showing 11–20 of 21');
    expect(fixture.nativeElement.textContent).toContain('EMP00011');
    expect(fixture.nativeElement.textContent).not.toContain('EMP00001');
  });

  it('disables navigation at both boundaries and does not issue out-of-range requests', async () => {
    await createApp((query) => of(page(query.page)));
    const app = fixture.componentInstance;
    const buttons = (): HTMLButtonElement[] => Array.from(fixture.nativeElement.querySelectorAll('.controls button'));

    expect(buttons()[0].disabled).toBe(true);
    expect(buttons()[1].disabled).toBe(false);
    app.loadPreviousPage();
    expect(getEmployees).toHaveBeenCalledTimes(1);

    app.loadNextPage();
    app.loadNextPage();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Page 3 of 3 · Showing 21–21 of 21');
    expect(buttons()[0].disabled).toBe(false);
    expect(buttons()[1].disabled).toBe(true);
    const requestsAtEnd = getEmployees.mock.calls.length;
    app.loadNextPage();
    expect(getEmployees).toHaveBeenCalledTimes(requestsAtEnd);
  });

  it('navigates back to the preceding zero-based page', async () => {
    await createApp((query) => of(page(query.page)));
    const app = fixture.componentInstance;

    app.loadNextPage();
    app.loadPreviousPage();
    fixture.detectChanges();

    expect(getEmployees).toHaveBeenNthCalledWith(2, { page: 1, size: 10, search: undefined });
    expect(getEmployees).toHaveBeenNthCalledWith(3, { page: 0, size: 10, search: undefined });
    expect(fixture.nativeElement.textContent).toContain('Page 1 of 3 · Showing 1–10 of 21');
    expect(fixture.nativeElement.textContent).toContain('EMP00001');
  });

  it('preserves search when navigating and resets to page zero when search changes', async () => {
    await createApp((query) => of(page(query.page)));
    const app = fixture.componentInstance;
    const search = fixture.nativeElement.querySelector('.search-bar input') as HTMLInputElement;
    search.value = 'engineer';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    app.loadNextPage();
    expect(getEmployees).toHaveBeenLastCalledWith({ page: 1, size: 10, search: 'engineer' });
    app.searchEmployees();
    expect(getEmployees).toHaveBeenLastCalledWith({ page: 0, size: 10, search: 'engineer' });
  });

  it('blocks repeated navigation while loading and does not show stale records', async () => {
    const pendingPage = new Subject<EmployeePage>();
    await createApp((query) => query.page === 0 ? of(page(0)) : pendingPage);
    const app = fixture.componentInstance;
    app.loadNextPage();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Loading page...');
    expect(fixture.nativeElement.textContent).not.toContain('EMP00001');
    expect((fixture.nativeElement.querySelector('.controls button:last-child') as HTMLButtonElement).disabled).toBe(true);
    app.loadNextPage();
    expect(getEmployees).toHaveBeenCalledTimes(2);

    pendingPage.next(page(1));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('EMP00011');
  });

  it('ignores late responses after a newer search request succeeds', async () => {
    const initialRequest = new Subject<EmployeePage>();
    const searchRequest = new Subject<EmployeePage>();
    await createApp(() => getEmployees.mock.calls.length === 1 ? initialRequest : searchRequest);
    const app = fixture.componentInstance;
    app.searchEmployees();
    searchRequest.next(page(0, [employee('EMP99999')]));
    initialRequest.next(page(0, [employee('EMP00001')]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('EMP99999');
    expect(fixture.nativeElement.textContent).not.toContain('EMP00001');
  });

  it('retains confirmed metadata and retries the failed zero-based page after an API transport error', async () => {
    await createApp((query) => query.page === 0
      ? of(page(0))
      : new Observable<EmployeePage>((subscriber) => subscriber.error(new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' }))));
    const app = fixture.componentInstance;

    app.loadNextPage();
    fixture.detectChanges();

    expect(getEmployees).toHaveBeenLastCalledWith({ page: 1, size: 10, search: undefined });
    expect(fixture.nativeElement.textContent).toContain('Page 1 of 3 · 21 employees');
    expect(fixture.nativeElement.textContent).toContain('Could not reach the employee API while loading page 2');
    expect(fixture.nativeElement.textContent).not.toContain('EMP00001');

    app.retryEmployees();
    expect(getEmployees).toHaveBeenLastCalledWith({ page: 1, size: 10, search: undefined });
  });

  it('refreshes analytics when country and department filters change and sends all active filters', async () => {
    await createApp((query) => of(page(query.page)), undefined, ['USD', 'INR']);
    fixture.detectChanges();

    const currency = fixture.nativeElement.querySelector('.analytics-heading select') as HTMLSelectElement;
    currency.value = 'INR';
    currency.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(getSalaryAnalytics).toHaveBeenLastCalledWith('INR', '', '');

    const country = fixture.nativeElement.querySelector('.analytics-filters label:first-child select') as HTMLSelectElement;
    country.value = 'India';
    country.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(getSalaryAnalytics).toHaveBeenLastCalledWith('INR', 'India', '');

    const department = fixture.nativeElement.querySelector('.analytics-filters label:nth-child(2) select') as HTMLSelectElement;
    department.value = 'Engineering';
    department.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(getSalaryAnalytics).toHaveBeenLastCalledWith('INR', 'India', 'Engineering');

    country.value = '';
    country.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(getSalaryAnalytics).toHaveBeenLastCalledWith('INR', '', 'Engineering');
    department.value = '';
    department.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(getSalaryAnalytics).toHaveBeenLastCalledWith('INR', '', '');
  });

  it('ignores late analytics responses and does not keep results from older filters', async () => {
    const indiaResponse = new Subject<SalaryAnalytics>();
    const canadaResponse = new Subject<SalaryAnalytics>();
    await createApp((query) => of(page(query.page)), (_currency, country) => country === 'India'
      ? indiaResponse
      : country === 'Canada' ? canadaResponse : of(analyticsResult()), ['INR']);

    const country = fixture.nativeElement.querySelector('.analytics-filters label:first-child select') as HTMLSelectElement;
    country.value = 'India';
    country.dispatchEvent(new Event('change'));
    country.value = 'Canada';
    country.dispatchEvent(new Event('change'));

    canadaResponse.next(analyticsResult('INR', ['Canada'], ['Engineering'], 1));
    indiaResponse.next(analyticsResult('INR', ['India'], ['Sales'], 1));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.analytics-summary').textContent).toContain('1');
    const countryRows = fixture.nativeElement.querySelector('.analytics-breakdowns section:first-child tbody').textContent;
    const departmentRows = fixture.nativeElement.querySelector('.analytics-breakdowns section:nth-child(2) tbody').textContent;
    expect(countryRows).toContain('Canada');
    expect(countryRows).not.toContain('India');
    expect(departmentRows).toContain('Engineering');
    expect(departmentRows).not.toContain('Sales');
  });

  it('shows zero totals and empty breakdown states for a no-match analytics response', async () => {
    await createApp((query) => of(page(query.page)), () => of(analyticsResult('INR', [], [], 0)), ['INR']);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.analytics-summary').textContent).toContain('0');
    expect(fixture.nativeElement.textContent).toContain('No matching employees for this currency and filter.');
    expect(fixture.nativeElement.querySelectorAll('.analytics-breakdowns table').length).toBe(0);
  });

  it('renders dynamic filter options with default All selections', async () => {
    await createApp((query) => of(page(query.page)), undefined, ['AUD', 'CAD', 'GBP', 'INR', 'USD'], () => of({
      countries: ['Australia', 'India'], departments: ['Engineering', 'Marketing'],
    }));
    const country = fixture.nativeElement.querySelector('.analytics-filters label:first-child select') as HTMLSelectElement;
    const department = fixture.nativeElement.querySelector('.analytics-filters label:nth-child(2) select') as HTMLSelectElement;

    expect(country.value).toBe('');
    expect(department.value).toBe('');
    expect(Array.from(country.options).map((option) => option.textContent?.trim())).toEqual([
      'All countries', 'Australia', 'India',
    ]);
    expect(Array.from(department.options).map((option) => option.textContent?.trim())).toEqual([
      'All departments', 'Engineering', 'Marketing',
    ]);
    expect(Array.from((fixture.nativeElement.querySelector('.analytics-heading select') as HTMLSelectElement).options)
      .map((option) => option.value)).toEqual(['AUD', 'CAD', 'GBP', 'INR', 'USD']);
  });

  it('shows filter-option loading state and enables dropdowns after the options arrive', async () => {
    const options = new Subject<SalaryAnalyticsFilterOptions>();
    await createApp((query) => of(page(query.page)), undefined, [], () => options);
    const country = fixture.nativeElement.querySelector('.analytics-filters label:first-child select') as HTMLSelectElement;
    expect(fixture.nativeElement.textContent).toContain('Loading country and department options...');
    expect(country.disabled).toBe(true);

    options.next({ countries: ['India'], departments: ['Engineering'] });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Loading country and department options...');
    expect(fixture.nativeElement.textContent).not.toContain('Failed to load country and department options.');
    const loadedCountry = fixture.nativeElement.querySelector('.analytics-filters label:first-child select') as HTMLSelectElement;
    expect(loadedCountry.disabled).toBe(false);
    expect(Array.from(loadedCountry.options).map((option) => option.textContent?.trim())).toEqual(['All countries', 'India']);
  });

  it('shows an options error and retry control instead of hardcoded fallback values', async () => {
    const failedOptions = new Observable<SalaryAnalyticsFilterOptions>((subscriber) => subscriber.error(new Error('offline')));
    await createApp((query) => of(page(query.page)), undefined, [], () => failedOptions);
    const country = fixture.nativeElement.querySelector('.analytics-filters label:first-child select') as HTMLSelectElement;

    expect(fixture.nativeElement.textContent).toContain('Failed to load country and department options. Please try again.');
    expect(fixture.nativeElement.querySelector('.analytics-error button').textContent).toContain('Retry options');
    expect(country.disabled).toBe(true);
    expect(Array.from(country.options).map((option) => option.textContent?.trim())).toEqual(['All countries']);
  });
});
