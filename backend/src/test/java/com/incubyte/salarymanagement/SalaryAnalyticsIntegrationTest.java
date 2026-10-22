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
    void currencyFiltersSummaryAndBothBreakdownsWithMixedCountryCurrencyData() throws Exception {
        saveMixedAnalyticsEmployees();

        mockMvc.perform(get("/api/analytics/salary").param("currency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(3)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(600.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(200.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(2)))
                .andExpect(jsonPath("$.byCountry[0].name", is("Canada")))
                .andExpect(jsonPath("$.byCountry[0].employeeCount", is(1)))
                .andExpect(jsonPath("$.byCountry[1].name", is("India")))
                .andExpect(jsonPath("$.byCountry[1].employeeCount", is(2)))
                .andExpect(jsonPath("$.byDepartment", hasSize(2)))
                .andExpect(jsonPath("$.byDepartment[0].name", is("Engineering")))
                .andExpect(jsonPath("$.byDepartment[0].employeeCount", is(2)))
                .andExpect(jsonPath("$.byDepartment[1].name", is("Sales")))
                .andExpect(jsonPath("$.byDepartment[1].employeeCount", is(1)));
    }

    @Test
    void indiaAndInrFilterSummaryAndBothBreakdownsTogether() throws Exception {
        saveMixedAnalyticsEmployees();

        mockMvc.perform(get("/api/analytics/salary").param("currency", "INR").param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(2)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(300.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(150.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(1)))
                .andExpect(jsonPath("$.byCountry[0].name", is("India")))
                .andExpect(jsonPath("$.byCountry[0].employeeCount", is(2)))
                .andExpect(jsonPath("$.byDepartment", hasSize(2)))
                .andExpect(jsonPath("$.byDepartment[0].name", is("Engineering")))
                .andExpect(jsonPath("$.byDepartment[0].employeeCount", is(1)))
                .andExpect(jsonPath("$.byDepartment[1].name", is("Sales")))
                .andExpect(jsonPath("$.byDepartment[1].employeeCount", is(1)));
    }

    @Test
    void indiaFilterFindsIndianEmployeesEvenWhenTheyUseDifferentCurrencies() throws Exception {
        saveMixedAnalyticsEmployees();

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD").param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(1)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(400.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(1)))
                .andExpect(jsonPath("$.byCountry[0].name", is("India")))
                .andExpect(jsonPath("$.byCountry[0].employeeCount", is(1)))
                .andExpect(jsonPath("$.byDepartment", hasSize(1)))
                .andExpect(jsonPath("$.byDepartment[0].name", is("Engineering")));
    }

    @Test
    void departmentFilterAppliesToSummaryAndBothBreakdowns() throws Exception {
        saveMixedAnalyticsEmployees();

        mockMvc.perform(get("/api/analytics/salary").param("currency", "INR").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(2)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(400.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(200.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(2)))
                .andExpect(jsonPath("$.byCountry[0].name", is("Canada")))
                .andExpect(jsonPath("$.byCountry[0].employeeCount", is(1)))
                .andExpect(jsonPath("$.byCountry[1].name", is("India")))
                .andExpect(jsonPath("$.byCountry[1].employeeCount", is(1)))
                .andExpect(jsonPath("$.byDepartment", hasSize(1)))
                .andExpect(jsonPath("$.byDepartment[0].name", is("Engineering")))
                .andExpect(jsonPath("$.byDepartment[0].employeeCount", is(2)));
    }

    @Test
    void countryDepartmentAndCurrencyFiltersApplyTogether() throws Exception {
        saveMixedAnalyticsEmployees();

        mockMvc.perform(get("/api/analytics/salary").param("currency", "INR")
                        .param("country", "India").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(1)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(100.00)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(100.00)))
                .andExpect(jsonPath("$.byCountry", hasSize(1)))
                .andExpect(jsonPath("$.byCountry[0].name", is("India")))
                .andExpect(jsonPath("$.byDepartment", hasSize(1)))
                .andExpect(jsonPath("$.byDepartment[0].name", is("Engineering")));
    }

    @Test
    void zeroMatchCountryDepartmentAndCurrencyReturnsZeroesAndEmptyBreakdowns() throws Exception {
        saveMixedAnalyticsEmployees();

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD")
                        .param("country", "India").param("department", "Legal"))
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
    void centLevelSalariesUseHalfUpRoundingForTheAverage() throws Exception {
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("CENT001", "cent1@example.test", "India", "Engineering", "USD", "0.01"),
                employee("CENT002", "cent2@example.test", "India", "Engineering", "USD", "0.02")
        ));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(2)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(0.03)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(0.02)))
                .andExpect(jsonPath("$.byCountry[0].totalAnnualSalary", is(0.03)))
                .andExpect(jsonPath("$.byCountry[0].averageAnnualSalary", is(0.02)))
                .andExpect(jsonPath("$.byDepartment[0].totalAnnualSalary", is(0.03)))
                .andExpect(jsonPath("$.byDepartment[0].averageAnnualSalary", is(0.02)));
    }

    @Test
    void largeCurrencyScopedTotalPreservesCents() throws Exception {
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("LARGE001", "large1@example.test", "India", "Engineering", "USD", "99999999.99"),
                employee("LARGE002", "large2@example.test", "India", "Engineering", "USD", "88888888.88"),
                employee("LARGE003", "large3@example.test", "India", "Engineering", "USD", "77777777.77"),
                employee("OTHER001", "other@example.test", "India", "Engineering", "EUR", "99999999.99")
        ));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(3)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(266666666.64)))
                .andExpect(jsonPath("$.averageAnnualSalary", is(88888888.88)))
                .andExpect(jsonPath("$.byCountry[0].totalAnnualSalary", is(266666666.64)))
                .andExpect(jsonPath("$.byDepartment[0].totalAnnualSalary", is(266666666.64)));
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

    @Test
    void filterOptionsReturnDistinctSortedNonBlankDatabaseValues() throws Exception {
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("OPT001", "opt1@example.test", "India", "Engineering", "INR", "100.00"),
                employee("OPT002", "opt2@example.test", "India", "Engineering", "INR", "200.00"),
                employee("OPT003", "opt3@example.test", "Canada", "Sales", "CAD", "300.00"),
                employee("OPT004", "opt4@example.test", "", "   ", "USD", "400.00")
        ));

        mockMvc.perform(get("/api/analytics/salary/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries", hasSize(2)))
                .andExpect(jsonPath("$.countries[0]", is("Canada")))
                .andExpect(jsonPath("$.countries[1]", is("India")))
                .andExpect(jsonPath("$.departments", hasSize(2)))
                .andExpect(jsonPath("$.departments[0]", is("Engineering")))
                .andExpect(jsonPath("$.departments[1]", is("Sales")));
    }

    @Test
    void filterOptionsAreEmptyWhenTheDatabaseHasNoEmployees() throws Exception {
        mockMvc.perform(get("/api/analytics/salary/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries", hasSize(0)))
                .andExpect(jsonPath("$.departments", hasSize(0)));
    }

    private Employee employee(String code, String email, String country, String department,
                              String currency, String salary) {
        return new Employee(code, email, "Test Employee", country, department, "Analyst", currency,
                new BigDecimal(salary));
    }

    private void saveMixedAnalyticsEmployees() {
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("INR001", "inr1@example.test", "India", "Engineering", "INR", "100.00"),
                employee("INR002", "inr2@example.test", "India", "Sales", "INR", "200.00"),
                employee("INR003", "inr3@example.test", "Canada", "Engineering", "INR", "300.00"),
                employee("USD001", "usd1@example.test", "India", "Engineering", "USD", "400.00"),
                employee("USD002", "usd2@example.test", "Canada", "Sales", "USD", "500.00"),
                employee("GBP001", "gbp1@example.test", "Canada", "Engineering", "GBP", "600.00")
        ));
    }
}
