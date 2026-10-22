package com.incubyte.salarymanagement.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeCode;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 10, scale = 2,
            columnDefinition = "NUMERIC(10,2) CHECK (annual_salary >= 0)")
    private BigDecimal annualSalary;

    @Column
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    // Constructors
    public Employee() {
    }

    public Employee(String employeeCode, String email, String fullName, String country,
                    String department, String jobTitle, String currency, BigDecimal annualSalary) {
        this.employeeCode = employeeCode;
        this.email = email;
        this.fullName = fullName;
        this.country = country;
        this.department = department;
        this.jobTitle = jobTitle;
        this.currency = currency;
        this.annualSalary = annualSalary;
    }

    /**
     * Constructor for testing purposes.
     * @param id the id
     * @param employeeCode the employee code
     * @param fullName the full name
     * @param email the email
     * @param country the country
     * @param department the department
     * @param jobTitle the job title
     * @param currency the currency
     * @param annualSalary the annual salary
     * @param createdAt the creation timestamp
     * @param updatedAt the update timestamp
     */
    @Deprecated
    public Employee(Long id, String employeeCode, String fullName, String email, String country,
                    String department, String jobTitle, String currency, BigDecimal annualSalary,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.email = email;
        this.country = country;
        this.department = department;
        this.jobTitle = jobTitle;
        this.currency = currency;
        this.annualSalary = annualSalary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAnnualSalary() {
        return annualSalary;
    }

    public void setAnnualSalary(BigDecimal annualSalary) {
        this.annualSalary = annualSalary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
