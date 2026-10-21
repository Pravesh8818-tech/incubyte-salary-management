package com.incubyte.salarymanagement.repository;

import com.incubyte.salarymanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(annual_salary), 0) FROM employee "
            + "WHERE currency = :currency "
            + "AND (:country IS NULL OR country = :country) "
            + "AND (:department IS NULL OR department = :department)", nativeQuery = true)
    List<Object[]> summarizeSalary(@Param("currency") String currency, @Param("country") String country,
                                   @Param("department") String department);

    @Query(value = "SELECT country, COUNT(*), COALESCE(SUM(annual_salary), 0) FROM employee "
            + "WHERE currency = :currency "
            + "AND (:country IS NULL OR country = :country) "
            + "AND (:department IS NULL OR department = :department) "
            + "GROUP BY country ORDER BY country", nativeQuery = true)
    List<Object[]> summarizeSalaryByCountry(@Param("currency") String currency,
                                            @Param("country") String country,
                                            @Param("department") String department);

    @Query(value = "SELECT department, COUNT(*), COALESCE(SUM(annual_salary), 0) FROM employee "
            + "WHERE currency = :currency "
            + "AND (:country IS NULL OR country = :country) "
            + "AND (:department IS NULL OR department = :department) "
            + "GROUP BY department ORDER BY department", nativeQuery = true)
    List<Object[]> summarizeSalaryByDepartment(@Param("currency") String currency,
                                               @Param("country") String country,
                                               @Param("department") String department);

    @Query(value = "SELECT DISTINCT currency FROM employee ORDER BY currency", nativeQuery = true)
    List<String> findDistinctCurrencies();
}
