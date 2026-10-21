import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeService } from './employee';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EmployeeService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('sends page index 1, page size 10, and the current search filter', () => {
    service.getEmployees({ page: 1, size: 10, search: ' Engineer ' }).subscribe();

    const request = httpTestingController.expectOne((req) => req.url === 'http://localhost:8080/api/employees');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('search')).toBe('Engineer');
    request.flush({ content: [], number: 1, size: 10, totalElements: 0, totalPages: 0, first: false, last: true, empty: true });
  });
});
