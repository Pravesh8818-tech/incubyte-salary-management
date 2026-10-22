package com.incubyte.salarymanagement.service;

public class EmployeeConflictException extends RuntimeException {
    public EmployeeConflictException() {
        super("Employee code or email already exists");
    }
}
