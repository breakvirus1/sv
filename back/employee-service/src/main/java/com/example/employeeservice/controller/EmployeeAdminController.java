package com.example.employeeservice.controller;

import com.example.common.entity.Employee;
import com.example.employeeservice.service.EmployeeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<List<Employee>> generateTestEmployees() {
        List<Employee> employees = employeeAdminService.generateTestEmployees(20);
        return ResponseEntity.ok(employees);
    }
}
