import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Employee {
  id: number;
  employeeCode: string;
  fullName: string;
  email: string;
  country: string;
  department: string;
  jobTitle: string;
  currency: string;
  annualSalary: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmployeePage {
  content: Employee[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface EmployeeQuery {
  page: number;
  size: number;
  search?: string;
  country?: string;
  department?: string;
  minSalary?: number;
  maxSalary?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface SalaryAnalyticsGroup {
  name: string;
  employeeCount: number;
  totalAnnualSalary: number;
  averageAnnualSalary: number;
}

export interface SalaryAnalytics {
  currency: string;
  employeeCount: number;
  totalAnnualSalary: number;
  averageAnnualSalary: number;
  byCountry: SalaryAnalyticsGroup[];
  byDepartment: SalaryAnalyticsGroup[];
}

@Injectable({
  providedIn: 'root',
})
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/employees';

  getEmployees(query: EmployeeQuery): Observable<EmployeePage> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);

    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.country) {
      params = params.set('country', query.country);
    }
    if (query.department) {
      params = params.set('department', query.department);
    }
    if (query.minSalary !== undefined) {
      params = params.set('minSalary', query.minSalary);
    }
    if (query.maxSalary !== undefined) {
      params = params.set('maxSalary', query.maxSalary);
    }
    if (query.sortBy) {
      params = params.set('sortBy', query.sortBy);
    }
    if (query.direction) {
      params = params.set('direction', query.direction);
    }

    return this.http.get<EmployeePage>(this.apiUrl, { params });
  }

  getSalaryCurrencies(): Observable<string[]> {
    return this.http.get<string[]>('http://localhost:8080/api/analytics/salary/currencies');
  }

  getSalaryAnalytics(currency: string, country?: string, department?: string): Observable<SalaryAnalytics> {
    let params = new HttpParams().set('currency', currency);
    if (country?.trim()) {
      params = params.set('country', country.trim());
    }
    if (department?.trim()) {
      params = params.set('department', department.trim());
    }
    return this.http.get<SalaryAnalytics>('http://localhost:8080/api/analytics/salary', { params });
  }
}
