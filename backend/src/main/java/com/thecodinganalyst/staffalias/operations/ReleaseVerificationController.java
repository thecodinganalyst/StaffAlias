package com.thecodinganalyst.staffalias.operations;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class ReleaseVerificationController {

    private final JdbcTemplate jdbcTemplate;

    public ReleaseVerificationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/database-readiness")
    public Map<String, String> databaseReadiness() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (result == null || result != 1) {
            throw new IllegalStateException("Database readiness query returned an unexpected result");
        }
        return Map.of("status", "UP");
    }
}
