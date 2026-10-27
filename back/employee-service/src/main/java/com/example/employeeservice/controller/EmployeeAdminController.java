package com.example.employeeservice.controller;

import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.service.EmployeeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Admin", description = "Админ API для управления сотрудниками")
public class EmployeeAdminController {

    private final EmployeeAdminService employeeAdminService;

    @Operation(summary = "Сгенерировать 20 тестовых сотрудников")
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Employee>> generateTestEmployees(@AuthenticationPrincipal Jwt jwt) {
        List<Employee> employees = employeeAdminService.generateTestEmployees(20, extractCompanyId(jwt));
        return ResponseEntity.ok(employees);
    }

    private Long extractCompanyId(Jwt jwt) {
        if (jwt == null) return null;
        try {
            Object claim = jwt.getClaim("company_id");
            if (claim instanceof Number) {
                return ((Number) claim).longValue();
            }
            if (claim instanceof String) {
                return Long.parseLong((String) claim);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
