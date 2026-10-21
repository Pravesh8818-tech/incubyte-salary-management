package com.incubyte.salarymanagement;

import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import com.incubyte.salarymanagement.seed.EmployeeSeedRunner;
import com.incubyte.salarymanagement.seed.EmployeeSeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "app.seed.enabled=false"
})
class EmployeeSeedServiceTest {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private EmployeeSeedService seedService;
    @Autowired private ObjectProvider<EmployeeSeedRunner> seedRunner;

    @BeforeEach
    void clearIsolatedDatabase() {
        employeeRepository.deleteAllInBatch();
    }

    @Test
    void freshDatabaseCanBeSeededWithTenThousandUniqueValidEmployees() {
        assertEquals(10_000, seedService.seed());
        assertEquals(10_000, employeeRepository.count());

        List<Employee> employees = employeeRepository.findAll();
        Set<String> codes = employees.stream().map(Employee::getEmployeeCode).collect(Collectors.toSet());
        Set<String> emails = employees.stream().map(Employee::getEmail).collect(Collectors.toSet());
        assertEquals(10_000, codes.size());
        assertEquals(10_000, emails.size());
        assertTrue(employees.stream().allMatch(e -> e.getEmployeeCode().matches("EMP\\d{5}")));
        assertTrue(employees.stream().allMatch(e -> e.getEmail().matches("employee\\d{5}@acme\\.example")));
        assertTrue(employees.stream().allMatch(e -> e.getFullName() != null && !e.getFullName().isBlank()
                && e.getCountry() != null && e.getDepartment() != null && e.getJobTitle() != null
                && e.getCurrency().matches("[A-Z]{3}") && e.getAnnualSalary().signum() >= 0
                && e.getAnnualSalary().scale() == 2));
    }

    @Test
    void repeatedSeedAbortsWithoutChangingExistingRecords() {
        seedService.seed();
        Employee first = employeeRepository.findAll().get(0);
        String originalName = first.getFullName();

        IllegalStateException error = assertThrows(IllegalStateException.class, seedService::seed);
        assertTrue(error.getMessage().contains("10000 employee(s)"));
        assertEquals(10_000, employeeRepository.count());
        assertEquals(originalName, employeeRepository.findById(first.getId()).orElseThrow().getFullName());
    }

    @Test
    void partiallyPopulatedDatabaseIsPreservedAndSeedIsRejected() {
        Employee existing = new Employee("CUSTOM", "custom@example.test", "Custom Person", "India",
                "Engineering", "Engineer", "INR", new java.math.BigDecimal("50000.00"));
        employeeRepository.saveAndFlush(existing);

        assertThrows(IllegalStateException.class, seedService::seed);
        assertEquals(1, employeeRepository.count());
        assertTrue(employeeRepository.existsById(existing.getId()));
    }

    @Test
    void normalStartupDoesNotRegisterSeedRunnerWhenDisabled() {
        assertNull(seedRunner.getIfAvailable());
        assertEquals(0, employeeRepository.count());
    }
}
