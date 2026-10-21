package com.incubyte.salarymanagement;

import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "app.seed.enabled=false"
})
class SalaryAnalyticsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeeRepository employeeRepository;

    @BeforeEach
    void clearIsolatedDatabase() {
        employeeRepository.deleteAllInBatch();
    }

    @Test
    void analyticsAreScopedToCurrencyAndIncludeCountryAndDepartmentBreakdowns() throws Exception {
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("USD001", "one@example.test", "USA", "Engineering", "USD", "60000.25"),
                employee("USD002", "two@example.test", "India", "Engineering", "USD", "80000.75"),
                employee("EUR001", "three@example.test", "USA", "Sales", "EUR", "90000.00")
        ));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.employeeCount", is(2)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(140001.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(70000.50)))
                .andExpect(jsonPath("$.byCountry", hasSize(2)))
                .andExpect(jsonPath("$.byCountry[0].name", is("India")))
                .andExpect(jsonPath("$.byCountry[0].totalAnnualSalary", is(80000.75)))
                .andExpect(jsonPath("$.byDepartment", hasSize(1)))
                .andExpect(jsonPath("$.byDepartment[0].name", is("Engineering")));
    }

    @Test
    void filtersApplyToSummaryAndBreakdownsAndNoMatchingDepartmentReturnsZeroes() throws Exception {
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("USD001", "one@example.test", "USA", "Engineering", "USD", "60000.25"),
                employee("USD002", "two@example.test", "India", "Engineering", "USD", "80000.75")
        ));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD")
                        .param("country", "USA").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(1)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(60000.25)))
                .andExpect(jsonPath("$.byCountry", hasSize(1)))
                .andExpect(jsonPath("$.byDepartment", hasSize(1)));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD").param("department", "Legal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(0)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(0.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(0.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(0)))
                .andExpect(jsonPath("$.byDepartment", hasSize(0)));
    }

    @Test
    void validCurrencyWithNoEmployeesReturnsAnEmptySummary() throws Exception {
        mockMvc.perform(get("/api/analytics/salary").param("currency", "JPY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(0)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(0.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(0.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(0)))
                .andExpect(jsonPath("$.byDepartment", hasSize(0)));
    }

    @Test
    void missingOrUnsupportedCurrencyIsRejected() throws Exception {
        mockMvc.perform(get("/api/analytics/salary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("currency is required")));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "QQQ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("currency must be a supported ISO 4217 three-letter code")));
    }

    private Employee employee(String code, String email, String country, String department,
                              String currency, String salary) {
        return new Employee(code, email, "Test Employee", country, department, "Analyst", currency,
                new BigDecimal(salary));
    }
}
