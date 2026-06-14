package com.example.orderservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void seed() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM svtables.orders WHERE deleted = false", Integer.class);
        if (count != null && count > 0) {
            log.info("Database already seeded ({} orders), skipping", count);
            return;
        }

        log.info("Seeding database...");
        try {
            ClassPathResource resource = new ClassPathResource("db/seed_full_data.sql");
            String sql = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

            for (String statement : sql.split("(?<=;)\\s*")) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                try {
                    jdbcTemplate.execute(trimmed);
                } catch (Exception e) {
                    if (trimmed.startsWith("DROP SCHEMA") || trimmed.startsWith("CREATE SCHEMA")) {
                        log.debug("Schema operation: {}", e.getMessage());
                    } else {
                        log.warn("Statement failed: {} - {}", trimmed.substring(0, Math.min(80, trimmed.length())), e.getMessage());
                    }
                }
            }

            Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM svtables.orders WHERE deleted = false", Integer.class);
            log.info("Seeding complete. Orders: {}", orderCount);
        } catch (Exception e) {
            log.error("Seeding failed", e);
        }
    }
}
