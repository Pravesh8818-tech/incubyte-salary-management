package com.incubyte.salarymanagement.dto;

import java.math.BigDecimal;

public class SalaryAnalyticsGroupDto {
    private final String name;
    private final long employeeCount;
    private final BigDecimal totalAnnualSalary;
    private final BigDecimal averageAnnualSalary;

    public SalaryAnalyticsGroupDto(String name, long employeeCount, BigDecimal totalAnnualSalary,
                                   BigDecimal averageAnnualSalary) {
        this.name = name;
        this.employeeCount = employeeCount;
        this.totalAnnualSalary = totalAnnualSalary;
        this.averageAnnualSalary = averageAnnualSalary;
    }

    public String getName() { return name; }
    public long getEmployeeCount() { return employeeCount; }
    public BigDecimal getTotalAnnualSalary() { return totalAnnualSalary; }
    public BigDecimal getAverageAnnualSalary() { return averageAnnualSalary; }
}
