import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL, resolveApiBaseUrl } from './api-config';
import { EmployeeService } from './employee';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: 'https://backend.example.test/api' }],
    });
    service = TestBed.inject(EmployeeService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('sends page index 1, page size 10, and the current search filter', () => {
    service.getEmployees({ page: 1, size: 10, search: ' Engineer ' }).subscribe();

    const request = httpTestingController.expectOne((req) => req.url === 'https://backend.example.test/api/employees');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('search')).toBe('Engineer');
    request.flush({ content: [], number: 1, size: 10, totalElements: 0, totalPages: 0, first: false, last: true, empty: true });
  });

  it('sends trimmed currency, country, and department analytics filters together', () => {
    service.getSalaryAnalytics('INR', ' India ', ' Engineering ').subscribe();

    const request = httpTestingController.expectOne((req) => req.url === 'https://backend.example.test/api/analytics/salary');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('currency')).toBe('INR');
    expect(request.request.params.get('country')).toBe('India');
    expect(request.request.params.get('department')).toBe('Engineering');
    request.flush({
      currency: 'INR', employeeCount: 1, totalAnnualSalary: 100.00, averageAnnualSalary: 100.00,
      byCountry: [{ name: 'India', employeeCount: 1, totalAnnualSalary: 100.00, averageAnnualSalary: 100.00 }],
      byDepartment: [{ name: 'Engineering', employeeCount: 1, totalAnnualSalary: 100.00, averageAnnualSalary: 100.00 }],
    });
  });

  it('loads database-backed salary filter options', () => {
    service.getSalaryFilterOptions().subscribe((options) => {
      expect(options.countries).toEqual(['Canada', 'India']);
      expect(options.departments).toEqual(['Engineering', 'Sales']);
    });

    const request = httpTestingController.expectOne('https://backend.example.test/api/analytics/salary/filters');
    expect(request.request.method).toBe('GET');
    request.flush({ countries: ['Canada', 'India'], departments: ['Engineering', 'Sales'] });
  });

  it('omits country and department parameters when both analytics filters are set to All', () => {
    service.getSalaryAnalytics('INR', '', '').subscribe();

    const request = httpTestingController.expectOne((req) => req.url === 'https://backend.example.test/api/analytics/salary');
    expect(request.request.params.keys()).toEqual(['currency']);
    expect(request.request.params.get('currency')).toBe('INR');
    request.flush({
      currency: 'INR', employeeCount: 3, totalAnnualSalary: 600, averageAnnualSalary: 200,
      byCountry: [], byDepartment: [],
    });
  });

  it('uses the existing employee routes for create, update, and delete', () => {
    const write = {
      employeeCode: 'EMP-1', fullName: 'Test Employee', email: 'test@example.test', country: 'India',
      department: 'Engineering', jobTitle: 'Developer', currency: 'INR', annualSalary: 123.45,
    };
    service.createEmployee(write).subscribe();
    const create = httpTestingController.expectOne('https://backend.example.test/api/employees');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(write);
    create.flush({ id: 1, ...write });

    service.updateEmployee(1, write).subscribe();
    const update = httpTestingController.expectOne('https://backend.example.test/api/employees/1');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(write);
    update.flush({ id: 1, ...write });

    service.deleteEmployee(1).subscribe();
    const deletion = httpTestingController.expectOne('https://backend.example.test/api/employees/1');
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
  });

  it('resolves and normalizes a public runtime API base URL', () => {
    expect(resolveApiBaseUrl({ apiBaseUrl: 'https://salary-api.example.test/api/' }))
      .toBe('https://salary-api.example.test/api');
    expect(resolveApiBaseUrl({ apiBaseUrl: '  ' })).toBe('http://localhost:8080/api');
  });
});
