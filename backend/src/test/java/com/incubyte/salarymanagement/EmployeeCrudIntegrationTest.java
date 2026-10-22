package com.incubyte.salarymanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "app.seed.enabled=false",
        "app.security.write-api-token=TEST_ONLY_FAKE_BEARER_TOKEN",
        "app.cors.allowed-origin=http://localhost:4200"
})
class EmployeeCrudIntegrationTest {
    private static final String TEST_ONLY_FAKE_BEARER_TOKEN = "TEST_ONLY_FAKE_BEARER_TOKEN";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearIsolatedDatabase() {
        employeeRepository.deleteAllInBatch();
    }

    @Test
    void createRetrieveUpdateAndDeleteAreReflectedInDirectoryAndAnalytics() throws Exception {
        String createBody = objectMapper.writeValueAsString(validEmployee());
        String created = mockMvc.perform(withWriteToken(post("/api/employees"))
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(jsonPath("$.employeeCode", is("EMP-CRUD-001")))
                .andExpect(jsonPath("$.annualSalary", is(12345.67)))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.email", is("crud@example.test")));

        Map<String, Object> updated = validEmployee();
        updated.put("fullName", "Updated Employee");
        updated.put("annualSalary", new BigDecimal("23456.78"));
        mockMvc.perform(withWriteToken(put("/api/employees/{id}", id))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Updated Employee")))
                .andExpect(jsonPath("$.employeeCode", is("EMP-CRUD-001")))
                .andExpect(jsonPath("$.annualSalary", is(23456.78)));

        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(1)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(23456.78)))
                .andExpect(jsonPath("$.byCountry[0].name", is("India")));

        mockMvc.perform(withWriteToken(delete("/api/employees/{id}", id))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/employees/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(0)))
                .andExpect(jsonPath("$.totalAnnualSalary", is(0.00)))
                .andExpect(jsonPath("$.byCountry.length()", is(0)))
                .andExpect(jsonPath("$.byDepartment.length()", is(0)));
    }

    @Test
    void employeeCreatePreflightAllowsLocalAngularOriginAndJsonHeaders() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                                org.hamcrest.Matchers.containsString("POST")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                                org.hamcrest.Matchers.containsString("authorization")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                                org.hamcrest.Matchers.containsString("content-type")));
    }

    @Test
    void employeeCreatePreflightRejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void employeeWriteEndpointsRejectMissingBearerTokens() throws Exception {
        String body = objectMapper.writeValueAsString(validEmployee());

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/employees/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerTokenIsRejectedWithGenericError() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmployee())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("A valid bearer token is required")));
    }

    @Test
    void queryParametersAndCookiesCannotAuthorizeEmployeeWrites() throws Exception {
        Map<String, Object> request = validEmployee();
        request.put("token", TEST_ONLY_FAKE_BEARER_TOKEN);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/employees")
                        .param("token", TEST_ONLY_FAKE_BEARER_TOKEN)
                        .cookie(new Cookie("Authorization", "Bearer " + TEST_ONLY_FAKE_BEARER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeListAndSalaryAnalyticsReadsRemainPublic() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/salary").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(0)));
    }

    @Test
    void createRejectsDuplicateEmployeeCodeAndEmailWithConflictResponses() throws Exception {
        employeeRepository.saveAndFlush(employeeEntity("EMP-DUP-001", "duplicate@example.test"));

        Map<String, Object> duplicateCode = validEmployee();
        duplicateCode.put("employeeCode", "EMP-DUP-001");
        duplicateCode.put("email", "other@example.test");
        assertConflict(duplicateCode);

        Map<String, Object> duplicateEmail = validEmployee();
        duplicateEmail.put("employeeCode", "EMP-DUP-002");
        duplicateEmail.put("email", "duplicate@example.test");
        assertConflict(duplicateEmail);
        org.junit.jupiter.api.Assertions.assertEquals(1, employeeRepository.count());
    }

    @Test
    void updateMayRetainOwnUniqueValuesAndRejectsValuesOwnedByAnotherEmployee() throws Exception {
        Employee first = employeeRepository.saveAndFlush(employeeEntity("EMP-UPDATE-001", "first@example.test"));
        Employee second = employeeRepository.saveAndFlush(employeeEntity("EMP-UPDATE-002", "second@example.test"));

        Map<String, Object> ownValues = requestFor(first);
        ownValues.put("fullName", "Changed Name");
        mockMvc.perform(withWriteToken(put("/api/employees/{id}", first.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownValues)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode", is("EMP-UPDATE-001")))
                .andExpect(jsonPath("$.email", is("first@example.test")));

        Map<String, Object> duplicate = requestFor(first);
        duplicate.put("employeeCode", second.getEmployeeCode());
        mockMvc.perform(withWriteToken(put("/api/employees/{id}", first.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Employee code or email already exists")));
    }

    @Test
    void invalidFieldsReturnFieldErrorsAndDoNotPersistEmployee() throws Exception {
        Map<String, Object> invalid = validEmployee();
        invalid.put("email", "not-an-email");
        invalid.put("annualSalary", new BigDecimal("-0.01"));
        invalid.put("country", " ");

        mockMvc.perform(withWriteToken(post("/api/employees"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.annualSalary").exists())
                .andExpect(jsonPath("$.fields.country").exists());
        org.junit.jupiter.api.Assertions.assertEquals(0, employeeRepository.count());
    }

    @Test
    void rejectsMissingFieldsInvalidCurrencyAndMalformedBodies() throws Exception {
        mockMvc.perform(withWriteToken(post("/api/employees"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.employeeCode").exists())
                .andExpect(jsonPath("$.fields.annualSalary").exists());

        Map<String, Object> unsupportedCurrency = validEmployee();
        unsupportedCurrency.put("currency", "QQQ");
        mockMvc.perform(withWriteToken(post("/api/employees")).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unsupportedCurrency)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.currency").exists());

        mockMvc.perform(withWriteToken(post("/api/employees"))
                        .contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Request body is invalid or incomplete")));
    }

    @Test
    void updateAndDeleteReturnNotFoundForMissingEmployees() throws Exception {
        String body = objectMapper.writeValueAsString(validEmployee());
        mockMvc.perform(withWriteToken(put("/api/employees/999")
                        .contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Employee not found with id: 999")));
        mockMvc.perform(withWriteToken(delete("/api/employees/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void generatedIdAndTimestampsCannotBeMassAssigned() throws Exception {
        Map<String, Object> request = validEmployee();
        request.put("id", 991L);
        request.put("createdAt", "2000-01-01T00:00:00Z");
        request.put("updatedAt", "2000-01-01T00:00:00Z");
        mockMvc.perform(withWriteToken(post("/api/employees")).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00Z")));
    }

    private void assertConflict(Map<String, Object> employee) throws Exception {
        mockMvc.perform(withWriteToken(post("/api/employees")).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Employee code or email already exists")));
    }

    private MockHttpServletRequestBuilder withWriteToken(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_ONLY_FAKE_BEARER_TOKEN);
    }

    private Map<String, Object> validEmployee() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("employeeCode", "EMP-CRUD-001");
        request.put("fullName", "CRUD Employee");
        request.put("email", "crud@example.test");
        request.put("country", "India");
        request.put("department", "Engineering");
        request.put("jobTitle", "Developer");
        request.put("currency", "USD");
        request.put("annualSalary", new BigDecimal("12345.67"));
        return request;
    }

    private Map<String, Object> requestFor(Employee employee) {
        Map<String, Object> request = validEmployee();
        request.put("employeeCode", employee.getEmployeeCode());
        request.put("email", employee.getEmail());
        return request;
    }

    private Employee employeeEntity(String code, String email) {
        return new Employee(code, email, "Test Employee", "India", "Engineering", "Developer", "USD",
                new BigDecimal("100.00"));
    }
}
