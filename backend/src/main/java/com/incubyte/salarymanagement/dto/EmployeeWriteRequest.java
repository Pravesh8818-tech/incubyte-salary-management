package com.incubyte.salarymanagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** Employee fields accepted from create and update requests. Database-managed fields are intentionally excluded. */
public class EmployeeWriteRequest {
    @NotBlank
    private String employeeCode;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String country;

    @NotBlank
    private String department;

    @NotBlank
    private String jobTitle;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}", message = "must be a three-letter uppercase ISO currency code")
    private String currency;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal annualSalary;

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAnnualSalary() { return annualSalary; }
    public void setAnnualSalary(BigDecimal annualSalary) { this.annualSalary = annualSalary; }
}
