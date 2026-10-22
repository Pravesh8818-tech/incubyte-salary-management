package com.incubyte.salarymanagement;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.PropertyValueException;
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

    /**
     * Extracts the root SQLException from a DataAccessException, unwrapping Hibernate JDBC exceptions.
     */
    private SQLException getRootSQLException(DataAccessException ex) {
        Throwable cause = ex.getCause();
        // Unwrap until we find a SQLException or exhaust the chain
        while (cause != null) {
            if (cause instanceof SQLException) {
                return (SQLException) cause;
            } else if (cause instanceof ConstraintViolationException || cause instanceof GenericJDBCException) {
                cause = cause.getCause();
            } else {
                cause = cause.getCause();
            }
        }
        return null;
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
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            employeeRepository.save(emp2);
            employeeRepository.flush();
        });
        SQLException sqlException = getRootSQLException(exception);
        assertNotNull(sqlException, "Expected root SQLException");
        String msg = sqlException.getMessage().toLowerCase();
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
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            employeeRepository.save(emp2);
            employeeRepository.flush();
        });
        SQLException sqlException = getRootSQLException(exception);
        assertNotNull(sqlException, "Expected root SQLException");
        String msg = sqlException.getMessage().toLowerCase();
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
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            employeeRepository.save(employee);
            employeeRepository.flush();
        });
        Throwable cause = exception;
        while (cause.getCause() != null && !(cause instanceof PropertyValueException) && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof PropertyValueException || cause instanceof SQLException,
                   "Expected PropertyValueException or SQLException, got: " + cause.getClass().getName());
        String msg = cause.getMessage().toLowerCase();
        assertTrue(msg.contains("not-null property references a null or transient value") ||
                   msg.contains("not null constraint failed") ||
                   msg.contains("null constraint failed"),
                "Expected not null violation message: " + msg);
    }

    @Test
    @Transactional
    void testTimestampsAutoPopulation() {
        // When timestamps are not set, they should be auto-populated on persist
        Employee employee = new Employee(
                "EMP007",
                "emp7@example.com",
                "Employee Seven",
                "USA",
                "Marketing",
                "Manager",
                "USD",
                new BigDecimal("75000.00")
        );
        // Do not set createdAt or updatedAt; they should be null initially
        assertNull(employee.getCreatedAt(), "createdAt should be null before save");
        assertNull(employee.getUpdatedAt(), "updatedAt should be null before save");

        Employee saved = employeeRepository.save(employee);
        assertNotNull(saved.getId());

        // After save, both timestamps should be non-null
        assertNotNull(saved.getCreatedAt(), "createdAt should be auto-populated on persist");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should be auto-populated on persist");
        // updatedAt should be greater than or equal to createdAt (since set together in @PrePersist)
        // Allow a small margin due to execution time; we just check that updatedAt is not before createdAt
        assertTrue(saved.getUpdatedAt().isAfter(saved.getCreatedAt()) ||
                   saved.getUpdatedAt().equals(saved.getCreatedAt()),
                "updatedAt should be after or equal to createdAt");

        // Also verify that the timestamps are persisted (retrieve again)
        Employee retrieved = employeeRepository.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getCreatedAt(), retrieved.getCreatedAt());
        assertEquals(saved.getUpdatedAt(), retrieved.getUpdatedAt());
    }

    @Test
    @Transactional
    void testUpdateUpdatesUpdatedAt() {
        // Create an employee and set explicit timestamps to verify they are respected on update
        Instant now = Instant.now();
        Employee employee = new Employee(
                "EMP008",
                "emp8@example.com",
                "Employee Eight",
                "USA",
                "Sales",
                "Rep",
                "USD",
                new BigDecimal("45000.00")
        );
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now); // same as created for now

        Employee saved = employeeRepository.save(employee);
        assertNotNull(saved.getId());
        // After first save, the timestamps should be as we set (since not null, PrePersist does not overwrite)
        assertEquals(now, saved.getCreatedAt(), "createdAt should remain as set");
        assertEquals(now, saved.getUpdatedAt(), "updatedAt should remain as set");

        // Now update the employee (change jobTitle) and save again
        Instant firstUpdate = saved.getUpdatedAt();
        employee.setJobTitle("Senior Rep");
        Employee updated = employeeRepository.save(employee);
        // Flush to persist changes and trigger @PreUpdate
        employeeRepository.flush();
        assertNotNull(updated.getId());
        // createdAt should remain unchanged
        assertEquals(now, updated.getCreatedAt(), "createdAt should remain unchanged on update");
        // updatedAt should be updated (greater than previous updatedAt)
        assertTrue(updated.getUpdatedAt().isAfter(firstUpdate),
                "updatedAt should be updated after modification");
        // Also ensure updatedAt is not equal to the previous updatedAt
        assertNotEquals(firstUpdate, updated.getUpdatedAt(), "updatedAt should change after update");
    }

    @Test
    @Transactional
    void testAnnualSalaryNonNegative() {
        // Since we added a check constraint for non-negative salary, we test that zero is allowed and negative is rejected.
        Employee employeeZero = new Employee(
                "EMP005",
                "emp5@example.com",
                "Employee Five",
                "USA",
                "Sales",
                "Sales Rep",
                "USD",
                BigDecimal.ZERO
        );
        Employee savedZero = employeeRepository.save(employeeZero);
        assertNotNull(savedZero.getId());
        assertEquals(BigDecimal.ZERO, savedZero.getAnnualSalary());

        Employee employeeNegative = new Employee(
                "EMP006",
                "emp6@example.com",
                "Employee Six",
                "USA",
                "Sales",
                "Sales Rep",
                "USD",
                new BigDecimal("-1000.00")
        );
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            employeeRepository.save(employeeNegative);
            employeeRepository.flush();
        });
        SQLException sqlException = getRootSQLException(exception);
        assertNotNull(sqlException, "Expected root SQLException");
        String msg = sqlException.getMessage().toLowerCase();
        assertTrue(msg.contains("check constraint failed") || msg.contains("constraint failed"),
                "Expected check constraint violation message: " + msg);
    }
}
