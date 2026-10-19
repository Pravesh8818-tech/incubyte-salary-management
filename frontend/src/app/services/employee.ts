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
}
