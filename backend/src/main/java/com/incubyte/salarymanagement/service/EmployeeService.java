package com.incubyte.salarymanagement.service;

import com.incubyte.salarymanagement.dto.EmployeeDto;
import com.incubyte.salarymanagement.dto.EmployeeWriteRequest;
import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Currency;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private EmployeeRepository employeeRepository;

    public Page<EmployeeDto> getEmployees(int page, int size, String sortBy, String direction,
                                          String search, String country, String department,
                                          String minSalaryStr, String maxSalaryStr) {
        // Validate and adjust pagination
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20; // default
        }
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        // Build Sort
        Sort sort = buildSort(sortBy, direction);

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // Build Specification
        Specification<Employee> spec = buildSpecification(
                search, country, department, minSalaryStr, maxSalaryStr);

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageRequest);

        // Map to DTO
        return employeePage.map(this::mapToDto);
    }

    private Sort buildSort(String sortBy, String direction) {
        // Whitelist of sortable fields
        String[] allowedFields = {
                "employeeCode", "fullName", "email", "country", "department",
                "jobTitle", "currency", "annualSalary", "createdAt", "updatedAt"
        };

        String finalSortBy = "employeeCode"; // default
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            String trimmed = sortBy.trim();
            for (String field : allowedFields) {
                if (field.equalsIgnoreCase(trimmed)) {
                    finalSortBy = field;
                    break;
                }
            }
        }

        String finalDirection = direction != null ? direction.trim().toLowerCase() : "asc";
        if (!finalDirection.equals("asc") && !finalDirection.equals("desc")) {
            finalDirection = "asc";
        }

        Sort sort = Sort.by(finalDirection.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, finalSortBy);
        // Add secondary sort by id to ensure deterministic ordering
        return sort.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Specification<Employee> buildSpecification(String search, String country, String department,
                                                       String minSalaryStr, String maxSalaryStr) {
        BigDecimal minSalary = null;
        BigDecimal maxSalary = null;

        if (minSalaryStr != null && !minSalaryStr.trim().isEmpty()) {
            try {
                minSalary = new BigDecimal(minSalaryStr.trim());
            } catch (NumberFormatException e) {
                // Invalid number, treat as null (will be ignored)
            }
        }
        if (maxSalaryStr != null && !maxSalaryStr.trim().isEmpty()) {
            try {
                maxSalary = new BigDecimal(maxSalaryStr.trim());
            } catch (NumberFormatException e) {
                // Invalid number, treat as null
            }
        }

        return new EmployeeSpecification(search, country, department, minSalary, maxSalary);
    }

    private EmployeeDto mapToDto(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getCountry(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getCurrency(),
                employee.getAnnualSalary(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    // For single employee retrieval
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id).orElse(null);
        if (employee == null) {
            return null;
        }
        return mapToDto(employee);
    }

    @Transactional
    public EmployeeDto createEmployee(EmployeeWriteRequest request) {
        validateCurrency(request.getCurrency());
        ensureUnique(request, null);
        try {
            return mapToDto(employeeRepository.saveAndFlush(toEntity(request)));
        } catch (RuntimeException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new EmployeeConflictException();
            }
            throw exception;
        }
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeWriteRequest request) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        validateCurrency(request.getCurrency());
        ensureUnique(request, id);
        apply(request, employee);
        try {
            return mapToDto(employeeRepository.saveAndFlush(employee));
        } catch (RuntimeException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new EmployeeConflictException();
            }
            throw exception;
        }
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        employeeRepository.delete(employee);
        employeeRepository.flush();
    }

    private void ensureUnique(EmployeeWriteRequest request, Long existingId) {
        boolean duplicateCode = existingId == null
                ? employeeRepository.existsByEmployeeCode(request.getEmployeeCode().trim())
                : employeeRepository.existsByEmployeeCodeAndIdNot(request.getEmployeeCode().trim(), existingId);
        boolean duplicateEmail = existingId == null
                ? employeeRepository.existsByEmail(request.getEmail().trim())
                : employeeRepository.existsByEmailAndIdNot(request.getEmail().trim(), existingId);
        if (duplicateCode || duplicateEmail) {
            throw new EmployeeConflictException();
        }
    }

    private void validateCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EmployeeValidationException("currency", "must be a supported ISO 4217 currency code");
        }
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                SQLException sqlException = (SQLException) cause;
                String message = sqlException.getMessage();
                if (sqlException.getErrorCode() == 19 && message != null
                        && message.toLowerCase(Locale.ROOT).contains("unique constraint failed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Employee toEntity(EmployeeWriteRequest request) {
        return new Employee(request.getEmployeeCode().trim(), request.getEmail().trim(),
                request.getFullName().trim(), request.getCountry().trim(), request.getDepartment().trim(),
                request.getJobTitle().trim(), request.getCurrency().trim().toUpperCase(Locale.ROOT),
                request.getAnnualSalary().setScale(2));
    }

    private void apply(EmployeeWriteRequest request, Employee employee) {
        employee.setEmployeeCode(request.getEmployeeCode().trim());
        employee.setEmail(request.getEmail().trim());
        employee.setFullName(request.getFullName().trim());
        employee.setCountry(request.getCountry().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT));
        employee.setAnnualSalary(request.getAnnualSalary().setScale(2));
    }
}
