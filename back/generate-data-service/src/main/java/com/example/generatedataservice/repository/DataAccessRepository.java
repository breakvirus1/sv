package com.example.generatedataservice.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DataAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    public DataAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> fetchClientIds() {
        return jdbcTemplate.query(
            "SELECT id FROM ordschema.clients WHERE deleted = false",
            (rs, rowNum) -> rs.getLong("id")
        );
    }

    public List<Long> fetchEmployeeIds() {
        return jdbcTemplate.query(
            "SELECT id FROM ordschema.employees WHERE deleted = false",
            (rs, rowNum) -> rs.getLong("id")
        );
    }

    public List<MaterialInfo> fetchMaterials() {
        return jdbcTemplate.query(
            "SELECT id, name, unit, price FROM ordschema.materials WHERE deleted = false",
            (rs, rowNum) -> new MaterialInfo(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("price")
            )
        );
    }

    public List<Long> fetchWorkshopIds() {
        return jdbcTemplate.query(
            "SELECT id FROM ordschema.workshops WHERE deleted = false",
            (rs, rowNum) -> rs.getLong("id")
        );
    }

    public List<Long> fetchOperationIds() {
        return jdbcTemplate.query(
            "SELECT id FROM calculator.calculator_operations WHERE deleted = false",
            (rs, rowNum) -> rs.getLong("id")
        );
    }

    public Long countOrders() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ordschema.orders WHERE deleted = false", Long.class);
        return count != null ? count : 0L;
    }

    public int deleteAllClients() {
        return jdbcTemplate.update("DELETE FROM ordschema.clients");
    }

    public int deleteAllEmployees() {
        return jdbcTemplate.update("DELETE FROM ordschema.employees");
    }

    public int deleteAllMaterials() {
        return jdbcTemplate.update("DELETE FROM ordschema.materials");
    }

    public int deleteAllWorkshops() {
        return jdbcTemplate.update("DELETE FROM ordschema.workshops");
    }

    public int deleteAllOperations() {
        return jdbcTemplate.update("DELETE FROM calculator.calculator_operations");
    }

    public int deleteAllOrders() {
        jdbcTemplate.update("DELETE FROM ordschema.order_materials WHERE order_id IN (SELECT id FROM ordschema.orders)");
        return jdbcTemplate.update("DELETE FROM ordschema.orders");
    }

    public static class MaterialInfo {
        private final Long id;
        private final String name;
        private final String unit;
        private final java.math.BigDecimal price;

        public MaterialInfo(Long id, String name, String unit, java.math.BigDecimal price) {
            this.id = id;
            this.name = name;
            this.unit = unit;
            this.price = price;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public java.math.BigDecimal getPrice() { return price; }
    }
}
