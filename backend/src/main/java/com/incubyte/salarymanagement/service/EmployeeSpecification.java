package com.incubyte.salarymanagement.service;

import com.incubyte.salarymanagement.entity.Employee;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification implements Specification<Employee> {

    private final String search;
    private final String country;
    private final String department;
    private final BigDecimal minSalary;
    private final BigDecimal maxSalary;

    public EmployeeSpecification(String search, String country, String department,
                                 BigDecimal minSalary, BigDecimal maxSalary) {
        this.search = search != null && !search.trim().isEmpty() ? search.trim() : null;
        this.country = country != null && !country.trim().isEmpty() ? country.trim() : null;
        this.department = department != null && !department.trim().isEmpty() ? department.trim() : null;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
    }

    @Override
    public Predicate toPredicate(Root<Employee> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Search across employeeCode, fullName, email (OR)
        if (search != null) {
            String likePattern = "%" + search.toLowerCase() + "%";
            Predicate codeLike = cb.like(cb.lower(root.get("employeeCode")), likePattern);
            Predicate nameLike = cb.like(cb.lower(root.get("fullName")), likePattern);
            Predicate emailLike = cb.like(cb.lower(root.get("email")), likePattern);
            predicates.add(cb.or(codeLike, nameLike, emailLike));
        }

        // Country exact match
        if (country != null) {
            predicates.add(cb.equal(root.get("country"), country));
        }

        // Department exact match
        if (department != null) {
            predicates.add(cb.equal(root.get("department"), department));
        }

        // Min salary
        if (minSalary != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("annualSalary"), minSalary));
        }

        // Max salary
        if (maxSalary != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("annualSalary"), maxSalary));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}