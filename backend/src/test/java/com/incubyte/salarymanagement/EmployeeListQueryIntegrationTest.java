package com.incubyte.salarymanagement;

import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

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
class EmployeeListQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void populateIsolatedDatabase() {
        employeeRepository.deleteAllInBatch();
        employeeRepository.saveAllAndFlush(Arrays.asList(
                employee("EMP001", "alice@example.test", "Alice Smith", "India", "Engineering", "USD", "50000.10"),
                employee("EMP002", "bob@example.test", "Bob Jones", "India", "Sales", "USD", "70000.25"),
                employee("EMP003", "carol@example.test", "Carol Lee", "Canada", "Engineering", "USD", "90000.50"),
                employee("EMP004", "dan@example.test", "Dan Roe", "USA", "Sales", "CAD", "70000.25"),
                employee("EMP005", "erin@example.test", "Erin Fox", "India", "Engineering", "USD", "30000.05")
        ));
    }

    @Test
    void searchMatchesEmployeeCodeNameAndEmailAgainstDatabaseRows() throws Exception {
        assertSearchFindsOnly("EMP003", "EMP003");
        assertSearchFindsOnly("Alice Smith", "EMP001");
        assertSearchFindsOnly("bob@example.test", "EMP002");
    }

    @Test
    void countryAndDepartmentFiltersRestrictDatabaseResults() throws Exception {
        mockMvc.perform(get("/api/employees").param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP002")))
                .andExpect(jsonPath("$.content[2].employeeCode", is("EMP005")));

        mockMvc.perform(get("/api/employees").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP003")))
                .andExpect(jsonPath("$.content[2].employeeCode", is("EMP005")));

        mockMvc.perform(get("/api/employees").param("country", "India").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP005")));
    }

    @Test
    void minimumAndMaximumSalaryBoundsAreInclusiveAndComposable() throws Exception {
        mockMvc.perform(get("/api/employees").param("minSalary", "70000.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP002")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP003")))
                .andExpect(jsonPath("$.content[2].employeeCode", is("EMP004")));

        mockMvc.perform(get("/api/employees").param("maxSalary", "50000.10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP005")));

        mockMvc.perform(get("/api/employees").param("minSalary", "50000.10").param("maxSalary", "70000.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP002")))
                .andExpect(jsonPath("$.content[2].employeeCode", is("EMP004")));
    }

    @Test
    void sortingUsesRequestedFieldAndDirectionWithDeterministicTies() throws Exception {
        mockMvc.perform(get("/api/employees").param("sortBy", "annualSalary").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP003")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP002")))
                .andExpect(jsonPath("$.content[2].employeeCode", is("EMP004")))
                .andExpect(jsonPath("$.content[3].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[4].employeeCode", is("EMP005")));

        mockMvc.perform(get("/api/employees").param("sortBy", "fullName").param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP002")))
                .andExpect(jsonPath("$.content[2].employeeCode", is("EMP003")));
    }

    @Test
    void paginationReturnsTheCorrectDatabaseSliceAndMetadata() throws Exception {
        mockMvc.perform(get("/api/employees").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.first", is(false)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP003")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP004")));
    }

    @Test
    void invalidParametersReturnBadRequest() throws Exception {
        assertBadRequest("page", "-1", "page must not be negative");
        assertBadRequest("size", "101", "size must not exceed 100");
        assertBadRequest("direction", "sideways", "Invalid sort direction");
        assertBadRequest("minSalary", "invalid", "Invalid minSalary value");

        mockMvc.perform(get("/api/employees").param("minSalary", "70000").param("maxSalary", "50000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("minSalary cannot be greater than maxSalary")));
    }

    @Test
    void filtersWithNoMatchesReturnAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/employees").param("country", "Japan").param("department", "Legal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(0)))
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)))
                .andExpect(jsonPath("$.empty", is(true)));
    }

    private void assertSearchFindsOnly(String search, String expectedCode) throws Exception {
        mockMvc.perform(get("/api/employees").param("search", search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].employeeCode", is(expectedCode)));
    }

    private void assertBadRequest(String parameter, String value, String expectedMessage) throws Exception {
        mockMvc.perform(get("/api/employees").param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString(expectedMessage)));
    }

    private Employee employee(String code, String email, String name, String country,
                              String department, String currency, String salary) {
        return new Employee(code, email, name, country, department, "Analyst", currency,
                new BigDecimal(salary));
    }
}
