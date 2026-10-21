package com.incubyte.salarymanagement.web;

import com.incubyte.salarymanagement.dto.EmployeeDto;
import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import com.incubyte.salarymanagement.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();
    }

    @Test
    void testGetEmployees_returnsPage() throws Exception {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2);
        Page<Employee> mockPage = new PageImpl<>(employeeList, PageRequest.of(0, 10), employeeList.size());
        Page<EmployeeDto> dtoPage = mockPage.map(emp -> new EmployeeDto(
                emp.getId(), emp.getEmployeeCode(), emp.getFullName(), emp.getEmail(),
                emp.getCountry(), emp.getDepartment(), emp.getJobTitle(), emp.getCurrency(),
                emp.getAnnualSalary(), emp.getCreatedAt(), emp.getUpdatedAt()));

        when(employeeService.getEmployees(eq(0), eq(10), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(dtoPage);

        // When & Then
        mockMvc.perform(get("/api/employees")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.content[1].employeeCode", is("EMP002")))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    void testGetEmployees_allowsAngularDevelopmentOrigin() throws Exception {
        Page<EmployeeDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);
        when(employeeService.getEmployees(eq(1), eq(10), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/employees")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Origin", "http://localhost:4200")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    void testGetEmployees_withSearch_returnsFiltered() throws Exception {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2);
        Page<Employee> mockPage = new PageImpl<>(employeeList, PageRequest.of(0, 20), employeeList.size());
        Page<EmployeeDto> dtoPage = mockPage.map(emp -> new EmployeeDto(
                emp.getId(), emp.getEmployeeCode(), emp.getFullName(), emp.getEmail(),
                emp.getCountry(), emp.getDepartment(), emp.getJobTitle(), emp.getCurrency(),
                emp.getAnnualSalary(), emp.getCreatedAt(), emp.getUpdatedAt()));

        when(employeeService.getEmployees(eq(0), eq(20), isNull(), isNull(),
                eq("John"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(dtoPage);

        // When & Then
        mockMvc.perform(get("/api/employees")
                        .param("search", "John")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2))) // In reality, service would filter, but we mock to return all; we trust service test for filtering logic.
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void testGetEmployees_withCountryFilter_returnsFiltered() throws Exception {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2);
        Page<Employee> mockPage = new PageImpl<>(employeeList, PageRequest.of(0, 20), employeeList.size());
        Page<EmployeeDto> dtoPage = mockPage.map(emp -> new EmployeeDto(
                emp.getId(), emp.getEmployeeCode(), emp.getFullName(), emp.getEmail(),
                emp.getCountry(), emp.getDepartment(), emp.getJobTitle(), emp.getCurrency(),
                emp.getAnnualSalary(), emp.getCreatedAt(), emp.getUpdatedAt()));

        when(employeeService.getEmployees(eq(0), eq(20), isNull(), isNull(),
                isNull(), eq("USA"), isNull(), isNull(), isNull()))
                .thenReturn(dtoPage);

        // When & Then
        mockMvc.perform(get("/api/employees")
                        .param("country", "USA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void testGetEmployees_withMinMaxSalary_returnsFiltered() throws Exception {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2);
        Page<Employee> mockPage = new PageImpl<>(employeeList, PageRequest.of(0, 20), employeeList.size());
        Page<EmployeeDto> dtoPage = mockPage.map(emp -> new EmployeeDto(
                emp.getId(), emp.getEmployeeCode(), emp.getFullName(), emp.getEmail(),
                emp.getCountry(), emp.getDepartment(), emp.getJobTitle(), emp.getCurrency(),
                emp.getAnnualSalary(), emp.getCreatedAt(), emp.getUpdatedAt()));

        when(employeeService.getEmployees(eq(0), eq(20), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("50000"), eq("60000")))
                .thenReturn(dtoPage);

        // When & Then
        mockMvc.perform(get("/api/employees")
                        .param("minSalary", "50000")
                        .param("maxSalary", "60000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void testGetEmployees_invalidPage_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("page must not be negative")));
    }

    @Test
    void testGetEmployees_invalidSize_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size must be greater than zero")));
    }

    @Test
    void testGetEmployees_sizeTooLarge_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("size", "101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size must not exceed 100")));
    }

    @Test
    void testGetEmployees_invalidSortDirection_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("direction", "invalid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort direction")));
    }

    @Test
    void testGetEmployees_invalidMinSalary_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("minSalary", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid minSalary value")));
    }

    @Test
    void testGetEmployees_minSalaryGreaterThanMaxSalary_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("minSalary", "60000")
                        .param("maxSalary", "50000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("minSalary cannot be greater than maxSalary")));
    }

    @Test
    void testGetEmployeeById_returnsEmployee() throws Exception {
        // Given
        Employee emp = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        EmployeeDto dto = new EmployeeDto(
                emp.getId(), emp.getEmployeeCode(), emp.getFullName(), emp.getEmail(),
                emp.getCountry(), emp.getDepartment(), emp.getJobTitle(), emp.getCurrency(),
                emp.getAnnualSalary(), emp.getCreatedAt(), emp.getUpdatedAt());

        when(employeeService.getEmployeeById(1L)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/employees/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.employeeCode", is("EMP001")))
                .andExpect(jsonPath("$.fullName", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));
    }

    @Test
    void testGetEmployeeById_notFound_returnsNotFound() throws Exception {
        when(employeeService.getEmployeeById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/employees/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Employee not found with id: 999")));
    }
}
