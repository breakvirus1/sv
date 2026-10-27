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

    public List<Long> fetchClientIdsByCompany(Long companyId) {
        return jdbcTemplate.query(
            "SELECT id FROM svschema.clients WHERE deleted = false AND company_id = ?",
            (rs, rowNum) -> rs.getLong("id"),
            companyId
        );
    }

    public List<Long> fetchEmployeeIdsByCompany(Long companyId) {
        return jdbcTemplate.query(
            "SELECT id FROM svschema.employees WHERE deleted = false AND company_id = ?",
            (rs, rowNum) -> rs.getLong("id"),
            companyId
        );
    }

    public List<MaterialInfo> fetchMaterialsByCompany(Long companyId) {
        return jdbcTemplate.query(
            "SELECT id, name, unit, price FROM svschema.materials WHERE deleted = false AND company_id = ?",
            (rs, rowNum) -> new MaterialInfo(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("price")
            ),
            companyId
        );
    }

    public List<Long> fetchWorkshopIdsByCompany(Long companyId) {
        return jdbcTemplate.query(
            "SELECT id FROM svschema.workshops WHERE deleted = false AND company_id = ?",
            (rs, rowNum) -> rs.getLong("id"),
            companyId
        );
    }

    public List<Long> fetchOperationIdsByCompany(Long companyId) {
        return jdbcTemplate.query(
            "SELECT id FROM svschema.calculator_operations WHERE deleted = false AND company_id = ?",
            (rs, rowNum) -> rs.getLong("id"),
            companyId
        );
    }

    public int deleteAllClients(Long companyId) {
        return jdbcTemplate.update("DELETE FROM svschema.clients WHERE company_id = ?", companyId);
    }

    public int deleteAllEmployees(Long companyId) {
        return jdbcTemplate.update("DELETE FROM svschema.employees WHERE company_id = ?", companyId);
    }

    public int deleteAllMaterials(Long companyId) {
        return jdbcTemplate.update("DELETE FROM svschema.materials WHERE company_id = ?", companyId);
    }

    public int deleteAllWorkshops(Long companyId) {
        return jdbcTemplate.update("DELETE FROM svschema.workshops WHERE company_id = ?", companyId);
    }

    public int deleteAllOperations(Long companyId) {
        return jdbcTemplate.update("DELETE FROM svschema.calculator_operations WHERE company_id = ?", companyId);
    }

    public int deleteAllOrders(Long companyId) {
        jdbcTemplate.update(
            "DELETE FROM svschema.order_materials WHERE order_id IN (SELECT id FROM svschema.orders WHERE company_id = ?)",
            companyId
        );
        return jdbcTemplate.update("DELETE FROM svschema.orders WHERE company_id = ?", companyId);
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
