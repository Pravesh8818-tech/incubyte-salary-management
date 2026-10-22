package com.incubyte.salarymanagement.service;

import com.incubyte.salarymanagement.dto.SalaryAnalyticsDto;
import com.incubyte.salarymanagement.dto.SalaryAnalyticsFilterOptionsDto;
import com.incubyte.salarymanagement.dto.SalaryAnalyticsGroupDto;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalaryAnalyticsService {
    private static final int MONEY_SCALE = 2;
    private final EmployeeRepository employeeRepository;

    public SalaryAnalyticsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public SalaryAnalyticsDto getSalaryAnalytics(String currency, String country, String department) {
        Object[] totals = employeeRepository.summarizeSalary(currency, normalize(country), normalize(department)).get(0);
        long employeeCount = ((Number) totals[0]).longValue();
        BigDecimal total = asMoneyFromCents(totals[1]);
        BigDecimal average = average(total, employeeCount);

        List<SalaryAnalyticsGroupDto> countries = mapGroups(employeeRepository.summarizeSalaryByCountry(
                currency, normalize(country), normalize(department)));
        List<SalaryAnalyticsGroupDto> departments = mapGroups(employeeRepository.summarizeSalaryByDepartment(
                currency, normalize(country), normalize(department)));

        return new SalaryAnalyticsDto(currency, employeeCount, total, average, countries, departments);
    }

    @Transactional(readOnly = true)
    public List<String> getCurrencies() {
        return employeeRepository.findDistinctCurrencies();
    }

    @Transactional(readOnly = true)
    public SalaryAnalyticsFilterOptionsDto getFilterOptions() {
        return new SalaryAnalyticsFilterOptionsDto(
                employeeRepository.findDistinctCountries(),
                employeeRepository.findDistinctDepartments());
    }

    private List<SalaryAnalyticsGroupDto> mapGroups(List<Object[]> rows) {
        List<SalaryAnalyticsGroupDto> groups = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            long count = ((Number) row[1]).longValue();
            BigDecimal total = asMoneyFromCents(row[2]);
            groups.add(new SalaryAnalyticsGroupDto((String) row[0], count, total, average(total, count)));
        }
        return groups;
    }

    private BigDecimal average(BigDecimal total, long count) {
        return count == 0 ? BigDecimal.ZERO.setScale(MONEY_SCALE)
                : total.divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal asMoneyFromCents(Object value) {
        BigDecimal cents = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
        return cents.movePointLeft(MONEY_SCALE).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private String normalize(String filter) {
        return filter == null || filter.trim().isEmpty() ? null : filter.trim();
    }
}
