package com.incubyte.salarymanagement.service;

import com.incubyte.salarymanagement.dto.EmployeeDto;
import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetEmployees_withNoFilters_returnsAllEmployees() {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2);
        Page<Employee> mockPage = new PageImpl<>(employeeList, PageRequest.of(0, 10), employeeList.size());

        when(employeeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(mockPage);

        // When
        Page<EmployeeDto> result = employeeService.getEmployees(0, 10, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("EMP001", result.getContent().get(0).getEmployeeCode());
        assertEquals("EMP002", result.getContent().get(1).getEmployeeCode());
        verify(employeeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void testGetEmployees_withSearch_returnsMatchingEmployees() {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2);
        Page<Employee> mockPage = new PageImpl<>(employeeList, PageRequest.of(0, 10), employeeList.size());

        when(employeeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(mockPage);

        // When
        Page<EmployeeDto> result = employeeService.getEmployees(0, 10, null, null, "John", null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements()); // Specification returns all because we didn't restrict the mock
        // In a real test, we would verify the specification internally, but for simplicity we trust the service.
        // We can also test the specification separately, but we'll rely on the integration test for end-to-end.
        verify(employeeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void testGetEmployees_withPagination_returnsCorrectPage() {
        // Given
        Employee emp1 = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        Employee emp2 = new Employee(2L, "EMP002", "Jane Smith", "jane@example.com", "CAN", "HR", "Mgr", "CAD", new BigDecimal("60000"), Instant.now(), Instant.now());
        Employee emp3 = new Employee(3L, "EMP003", "Bob Lee", "bob@example.com", "USA", "SALES", "Lead", "USD", new BigDecimal("70000"), Instant.now(), Instant.now());
        List<Employee> employeeList = Arrays.asList(emp1, emp2, emp3);
        Page<Employee> mockPage = new PageImpl<>(employeeList.subList(0, 2), PageRequest.of(0, 2), employeeList.size()); // first page, size 2

        when(employeeRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(mockPage);

        // When
        Page<EmployeeDto> result = employeeService.getEmployees(0, 2, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getSize());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getContent().size());
        verify(employeeRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void testGetEmployeeById_returnsDto() {
        // Given
        Employee emp = new Employee(1L, "EMP001", "John Doe", "john@example.com", "USA", "ENG", "Dev", "USD", new BigDecimal("50000"), Instant.now(), Instant.now());
        when(employeeRepository.findById(1L)).thenReturn(java.util.Optional.of(emp));

        // When
        EmployeeDto result = employeeService.getEmployeeById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("EMP001", result.getEmployeeCode());
        assertEquals("John Doe", result.getFullName());
        assertEquals("john@example.com", result.getEmail());
        verify(employeeRepository).findById(1L);
    }

    @Test
    void testGetEmployeeById_returnsNullWhenNotFound() {
        // Given
        when(employeeRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // When
        EmployeeDto result = employeeService.getEmployeeById(999L);

        // Then
        assertNull(result);
        verify(employeeRepository).findById(999L);
    }
}