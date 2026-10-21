package com.incubyte.salarymanagement.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class EmployeeSeedRunner implements ApplicationRunner {

    private final EmployeeSeedService seedService;

    public EmployeeSeedRunner(EmployeeSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int seeded = seedService.seed();
        System.out.println("Employee seed complete: " + seeded + " records inserted.");
    }
}
