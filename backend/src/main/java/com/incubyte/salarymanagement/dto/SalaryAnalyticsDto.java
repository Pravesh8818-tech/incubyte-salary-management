package com.incubyte.salarymanagement.dto;

import java.math.BigDecimal;
import java.util.List;

public class SalaryAnalyticsDto {
    private final String currency;
    private final long employeeCount;
    private final BigDecimal totalAnnualSalary;
    private final BigDecimal averageAnnualSalary;
    private final List<SalaryAnalyticsGroupDto> byCountry;
    private final List<SalaryAnalyticsGroupDto> byDepartment;

    public SalaryAnalyticsDto(String currency, long employeeCount, BigDecimal totalAnnualSalary,
                              BigDecimal averageAnnualSalary, List<SalaryAnalyticsGroupDto> byCountry,
                              List<SalaryAnalyticsGroupDto> byDepartment) {
        this.currency = currency;
        this.employeeCount = employeeCount;
        this.totalAnnualSalary = totalAnnualSalary;
        this.averageAnnualSalary = averageAnnualSalary;
        this.byCountry = byCountry;
        this.byDepartment = byDepartment;
    }

    public String getCurrency() { return currency; }
    public long getEmployeeCount() { return employeeCount; }
    public BigDecimal getTotalAnnualSalary() { return totalAnnualSalary; }
    public BigDecimal getAverageAnnualSalary() { return averageAnnualSalary; }
    public List<SalaryAnalyticsGroupDto> getByCountry() { return byCountry; }
    public List<SalaryAnalyticsGroupDto> getByDepartment() { return byDepartment; }
}
