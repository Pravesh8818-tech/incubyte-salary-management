package com.incubyte.salarymanagement.seed;

import com.incubyte.salarymanagement.entity.Employee;
import com.incubyte.salarymanagement.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeSeedService {

    public static final int EMPLOYEE_COUNT = 10_000;
    private static final int BATCH_SIZE = 500;
    private static final String[] FIRST_NAMES = {
            "Alex", "Amara", "Ava", "Ben", "Chen", "Daniel", "Elena", "Fatima",
            "Grace", "Hiro", "Isabel", "Jamal", "Liam", "Maya", "Noah", "Priya",
            "Ravi", "Sofia", "Theo", "Zara"
    };
    private static final String[] LAST_NAMES = {
            "Adams", "Brown", "Chen", "Davis", "Evans", "Garcia", "Hughes", "Iyer",
            "Jones", "Kim", "Lopez", "Miller", "Nakamura", "Patel", "Singh", "Wilson"
    };
    private static final String[] COUNTRIES = {"United States", "India", "United Kingdom", "Canada", "Australia"};
    private static final String[] DEPARTMENTS = {"Engineering", "Finance", "Human Resources", "Marketing", "Operations", "Sales", "Product", "Customer Support"};
    private static final String[] JOB_TITLES = {"Software Engineer", "Data Analyst", "Product Manager", "Accountant", "HR Specialist", "Marketing Manager", "Operations Analyst", "Sales Executive"};
    private static final String[] CURRENCIES = {"USD", "INR", "GBP", "CAD", "AUD"};

    private final EmployeeRepository employeeRepository;

    public EmployeeSeedService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /** Seeds an empty database. Existing data is always preserved. */
    @Transactional
    public int seed() {
        long existingCount = employeeRepository.count();
        if (existingCount != 0) {
            throw new IllegalStateException("Seed aborted: database already contains " + existingCount
                    + " employee(s); no records were changed.");
        }

        for (int start = 1; start <= EMPLOYEE_COUNT; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, EMPLOYEE_COUNT + 1);
            List<Employee> batch = new ArrayList<>(end - start);
            for (int number = start; number < end; number++) {
                batch.add(createEmployee(number));
            }
            employeeRepository.saveAll(batch);
            employeeRepository.flush();
        }
        return EMPLOYEE_COUNT;
    }

    private Employee createEmployee(int number) {
        String code = String.format("EMP%05d", number);
        String firstName = FIRST_NAMES[(number - 1) % FIRST_NAMES.length];
        String lastName = LAST_NAMES[((number - 1) / FIRST_NAMES.length) % LAST_NAMES.length];
        String email = String.format("employee%05d@acme.example", number);
        String country = COUNTRIES[(number - 1) % COUNTRIES.length];
        String department = DEPARTMENTS[((number - 1) / COUNTRIES.length) % DEPARTMENTS.length];
        String title = JOB_TITLES[((number - 1) / DEPARTMENTS.length) % JOB_TITLES.length];
        String currency = CURRENCIES[(number - 1) % CURRENCIES.length];
        long wholeDollars = 35_000L + (number * 137L) % 115_000L;
        long cents = ((number * 37L) % 9L + 1L) + 10L * ((number * 17L) % 9L);
        BigDecimal salary = BigDecimal.valueOf(wholeDollars).add(BigDecimal.valueOf(cents, 2));
        return new Employee(code, email, firstName + " " + lastName, country, department, title, currency, salary);
    }
}
