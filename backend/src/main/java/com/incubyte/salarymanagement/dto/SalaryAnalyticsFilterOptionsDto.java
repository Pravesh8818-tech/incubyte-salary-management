package com.incubyte.salarymanagement.dto;

import java.util.List;

public class SalaryAnalyticsFilterOptionsDto {
    private final List<String> countries;
    private final List<String> departments;

    public SalaryAnalyticsFilterOptionsDto(List<String> countries, List<String> departments) {
        this.countries = countries;
        this.departments = departments;
    }

    public List<String> getCountries() {
        return countries;
    }

    public List<String> getDepartments() {
        return departments;
    }
}
