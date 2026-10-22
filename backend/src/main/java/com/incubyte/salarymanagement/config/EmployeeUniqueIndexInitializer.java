package com.incubyte.salarymanagement.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates SQLite-compatible unique indexes for employee identifiers. */
@Component
public class EmployeeUniqueIndexInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public EmployeeUniqueIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_employee_code " +
                "ON employee (employee_code)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_email " +
                "ON employee (email)");
    }
}
