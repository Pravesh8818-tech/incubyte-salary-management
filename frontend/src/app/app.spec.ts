import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, Subject } from 'rxjs';
import { App } from './app';
import { Employee, EmployeePage, EmployeeQuery, EmployeeService } from './services/employee';

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

describe('App employee pagination', () => {
  let getEmployees: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<App>;

  async function createApp(employeeRequest: (query: EmployeeQuery) => Observable<EmployeePage>) {
    getEmployees = vi.fn(employeeRequest);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{
        provide: EmployeeService,
        useValue: {
          getEmployees,
          getSalaryCurrencies: vi.fn(() => of([])),
          getSalaryAnalytics: vi.fn(),
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
});
