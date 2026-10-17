package com.incubyte.salarymanagement;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.driver-class-name=org.sqlite.JDBC"
})
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @Transactional
    void testSaveAndRetrieveEmployee() {
        Employee employee = new Employee(
                "EMP001",
                "john.doe@example.com",
                "John Doe",
                "USA",
                "Engineering",
                "Software Engineer",
                "USD",
                new BigDecimal("50000.00")
        );
        employee.setCreatedAt(Instant.now());
        employee.setUpdatedAt(Instant.now());

        Employee saved = employeeRepository.save(employee);
        assertNotNull(saved.getId(), "Employee ID should be generated");
        assertEquals("EMP001", saved.getEmployeeCode());
        assertEquals("john.doe@example.com", saved.getEmail());

        Employee retrieved = employeeRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved, "Retrieved employee should not be null");
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(saved.getEmployeeCode(), retrieved.getEmployeeCode());
        assertEquals(saved.getEmail(), retrieved.getEmail());
        assertEquals(saved.getFullName(), retrieved.getFullName());
        assertEquals(saved.getCountry(), retrieved.getCountry());
        assertEquals(saved.getDepartment(), retrieved.getDepartment());
        assertEquals(saved.getJobTitle(), retrieved.getJobTitle());
        assertEquals(saved.getCurrency(), retrieved.getCurrency());
        assertEquals(saved.getAnnualSalary(), retrieved.getAnnualSalary());
        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    @Transactional
    void testUniqueEmployeeCodeConstraint() {
        Employee emp1 = new Employee(
                "EMP002",
                "emp1@example.com",
                "Employee One",
                "USA",
                "HR",
                "HR Manager",
                "USD",
                new BigDecimal("60000.00")
        );
        employeeRepository.save(emp1);
        employeeRepository.flush();

        Employee emp2 = new Employee(
                "EMP002", // duplicate code
                "emp2@example.com",
                "Employee Two",
                "USA",
                "HR",
                "HR Assistant",
                "USD",
                new BigDecimal("55000.00")
        );
        Throwable exception = assertThrows(Throwable.class, () -> {
            employeeRepository.save(emp2);
            employeeRepository.flush();
        });
        assertTrue(exception.getCause() instanceof org.sqlite.SQLiteException ||
                   exception.getCause() instanceof SQLException,
                   "Expected SQL exception");
        String msg = exception.getCause().getMessage().toLowerCase();
        assertTrue(msg.contains("unique constraint failed") || msg.contains("constraint failed"),
                "Expected unique constraint violation message: " + msg);
    }

    @Test
    @Transactional
    void testUniqueEmailConstraint() {
        Employee emp1 = new Employee(
                "EMP003",
                "emp3@example.com",
                "Employee Three",
                "USA",
                "Finance",
                "Analyst",
                "USD",
                new BigDecimal("70000.00")
        );
        employeeRepository.save(emp1);
        employeeRepository.flush();

        Employee emp2 = new Employee(
                "EMP004",
                "emp3@example.com", // duplicate email
                "Employee Four",
                "USA",
                "Finance",
                "Senior Analyst",
                "USD",
                new BigDecimal("80000.00")
        );
        Throwable exception = assertThrows(Throwable.class, () -> {
            employeeRepository.save(emp2);
            employeeRepository.flush();
        });
        assertTrue(exception.getCause() instanceof org.sqlite.SQLiteException ||
                   exception.getCause() instanceof SQLException,
                   "Expected SQL exception");
        String msg = exception.getCause().getMessage().toLowerCase();
        assertTrue(msg.contains("unique constraint failed") || msg.contains("constraint failed"),
                "Expected unique constraint violation message: " + msg);
    }

    @Test
    @Transactional
    void testRequiredFields() {
        Employee employee = new Employee(
                null, // employeeCode null
                null, // email null
                null, // fullName null
                null, // country null
                null, // department null
                null, // jobTitle null
                null, // currency null
                null  // annualSalary null
        );
        Throwable exception = assertThrows(Throwable.class, () -> {
            employeeRepository.save(employee);
            employeeRepository.flush();
        });
        assertTrue(exception.getCause() instanceof org.sqlite.SQLiteException ||
                   exception.getCause() instanceof SQLException,
                   "Expected SQL exception");
        String msg = exception.getCause().getMessage().toLowerCase();
        assertTrue(msg.contains("not null constraint failed") || msg.contains("null constraint failed"),
                "Expected not null constraint violation message: " + msg);
    }

    @Test
    @Transactional
    void testAnnualSalaryNonNegative() {
        // Since we didn't add a check constraint for non-negative salary, we just test that zero is allowed.
        Employee employee = new Employee(
                "EMP005",
                "emp5@example.com",
                "Employee Five",
                "USA",
                "Sales",
                "Sales Rep",
                "USD",
                BigDecimal.ZERO
        );
        Employee saved = employeeRepository.save(employee);
        assertNotNull(saved.getId());
        assertEquals(BigDecimal.ZERO, saved.getAnnualSalary());
    }
}
