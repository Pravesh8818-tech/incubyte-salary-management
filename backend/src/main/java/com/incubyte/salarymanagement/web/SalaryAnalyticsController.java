package com.incubyte.salarymanagement.web;

import com.incubyte.salarymanagement.dto.SalaryAnalyticsDto;
import com.incubyte.salarymanagement.dto.SalaryAnalyticsFilterOptionsDto;
import com.incubyte.salarymanagement.service.SalaryAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/analytics/salary")
public class SalaryAnalyticsController {
    private final SalaryAnalyticsService salaryAnalyticsService;

    public SalaryAnalyticsController(SalaryAnalyticsService salaryAnalyticsService) {
        this.salaryAnalyticsService = salaryAnalyticsService;
    }

    @GetMapping
    public ResponseEntity<?> getSalaryAnalytics(@RequestParam(required = false) String currency,
                                                @RequestParam(required = false) String country,
                                                @RequestParam(required = false) String department) {
        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (normalizedCurrency.isEmpty()) {
            return badRequest("currency is required");
        }
        try {
            if (!normalizedCurrency.matches("[A-Z]{3}")) {
                return badRequest("currency must be a supported ISO 4217 three-letter code");
            }
            Currency.getInstance(normalizedCurrency);
        } catch (IllegalArgumentException exception) {
            return badRequest("currency must be a supported ISO 4217 three-letter code");
        }
        return ResponseEntity.ok(salaryAnalyticsService.getSalaryAnalytics(normalizedCurrency, country, department));
    }

    @GetMapping("/currencies")
    public List<String> getCurrencies() {
        return salaryAnalyticsService.getCurrencies();
    }

    @GetMapping("/filters")
    public SalaryAnalyticsFilterOptionsDto getFilterOptions() {
        return salaryAnalyticsService.getFilterOptions();
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", Instant.now().toString());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }
}
