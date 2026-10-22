package com.incubyte.salarymanagement.web;

import com.incubyte.salarymanagement.dto.EmployeeDto;
import com.incubyte.salarymanagement.dto.EmployeeWriteRequest;
import com.incubyte.salarymanagement.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<?> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String minSalary,
            @RequestParam(required = false) String maxSalary) {

        // Validate pagination
        if (page < 0) {
            return badRequest("page must not be negative");
        }
        if (size <= 0) {
            return badRequest("size must be greater than zero");
        }
        if (size > 100) {
            return badRequest("size must not exceed 100");
        }

        // Validate sort direction
        if (direction != null && !direction.isEmpty() && !direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            return badRequest("Invalid sort direction: " + direction);
        }

        // Validate salary parameters
        if (minSalary != null && !minSalary.isEmpty()) {
            try {
                new java.math.BigDecimal(minSalary);
            } catch (NumberFormatException e) {
                return badRequest("Invalid minSalary value");
            }
        }
        if (maxSalary != null && !maxSalary.isEmpty()) {
            try {
                new java.math.BigDecimal(maxSalary);
            } catch (NumberFormatException e) {
                return badRequest("Invalid maxSalary value");
            }
        }
        if (minSalary != null && !minSalary.isEmpty() && maxSalary != null && !maxSalary.isEmpty()) {
            try {
                BigDecimal min = new BigDecimal(minSalary);
                BigDecimal max = new BigDecimal(maxSalary);
                if (min.compareTo(max) > 0) {
                    return badRequest("minSalary cannot be greater than maxSalary");
                }
            } catch (NumberFormatException e) {
                // Already caught above
            }
        }

        Page<EmployeeDto> employeePage = employeeService.getEmployees(
                page, size, sortBy, direction, search, country, department, minSalary, maxSalary);

        return ResponseEntity.ok(employeePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long id) {
        EmployeeDto employee = employeeService.getEmployeeById(id);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(HttpStatus.NOT_FOUND.value(), "Employee not found with id: " + id));
        }
        return ResponseEntity.ok(employee);
    }

    @PostMapping
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeWriteRequest request) {
        EmployeeDto employee = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long id,
                                                      @Valid @RequestBody EmployeeWriteRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    private Map<String, Object> errorResponse(int status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", java.time.Instant.now().toString());
        error.put("status", status);
        error.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        error.put("message", message);
        // Note: path is not included here for simplicity; could be added if needed
        return error;
    }
}
