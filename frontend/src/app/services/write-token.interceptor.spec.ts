import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-config';
import { writeTokenInterceptor } from './write-token.interceptor';
import { WriteTokenService } from './write-token';

describe('writeTokenInterceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let tokens: WriteTokenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: API_BASE_URL, useValue: 'https://backend.example.test/api' },
        provideHttpClient(withInterceptors([writeTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(WriteTokenService);
  });

  afterEach(() => requests.verify());

  it.each([
    ['POST', (client: HttpClient) => client.post('https://backend.example.test/api/employees', {})],
    ['PUT', (client: HttpClient) => client.put('https://backend.example.test/api/employees/7', {})],
    ['DELETE', (client: HttpClient) => client.delete('https://backend.example.test/api/employees/7')],
  ])('attaches a bearer token to authorized %s employee writes', (_method, makeRequest) => {
    tokens.setToken('fake-assessment-token');
    makeRequest(http).subscribe();

    const request = requests.match((req) => req.method !== 'GET')[0];
    expect(request.request.headers.getAll('Authorization')).toEqual(['Bearer fake-assessment-token']);
    request.flush({});
  });

  it('does not add authorization when no token is entered', () => {
    http.post('https://backend.example.test/api/employees', {}).subscribe();
    const request = requests.expectOne('https://backend.example.test/api/employees');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('does not add authorization to GET requests', () => {
    tokens.setToken('fake-assessment-token');
    http.get('https://backend.example.test/api/employees').subscribe();
    const request = requests.expectOne('https://backend.example.test/api/employees');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('never forwards the token to another origin or outside the API path', () => {
    tokens.setToken('fake-assessment-token');
    http.post('https://other.example.test/api/employees', {}).subscribe();
    http.post('https://backend.example.test/other/employees', {}).subscribe();

    const external = requests.expectOne('https://other.example.test/api/employees');
    const unrelated = requests.expectOne('https://backend.example.test/other/employees');
    expect(external.request.headers.has('Authorization')).toBe(false);
    expect(unrelated.request.headers.has('Authorization')).toBe(false);
    external.flush({});
    unrelated.flush({});
  });

  it('stops sending the token after it is cleared', () => {
    tokens.setToken('fake-assessment-token');
    tokens.clearToken();
    http.delete('https://backend.example.test/api/employees/7').subscribe();
    const request = requests.expectOne('https://backend.example.test/api/employees/7');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(null);
  });

  it('preserves an existing authorization header without adding a duplicate', () => {
    tokens.setToken('fake-assessment-token');
    http.post('https://backend.example.test/api/employees', {}, {
      headers: { Authorization: 'Bearer caller-supplied-token' },
    }).subscribe();
    const request = requests.expectOne('https://backend.example.test/api/employees');
    expect(request.request.headers.getAll('Authorization')).toEqual(['Bearer caller-supplied-token']);
    request.flush({});
  });
});
