package com.incubyte.salarymanagement.service;

import java.util.Collections;
import java.util.Map;

public class EmployeeValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public EmployeeValidationException(String field, String message) {
        super("Validation failed");
        this.fieldErrors = Collections.singletonMap(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
