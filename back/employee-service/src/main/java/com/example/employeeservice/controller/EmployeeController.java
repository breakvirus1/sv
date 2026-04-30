package com.example.employeeservice.controller;

import com.example.common.entity.Employee;
import com.example.employeeservice.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Service", description = "API для управления сотрудниками")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Получить список сотрудников")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<Employee>> getAllEmployees(
            @RequestParam(required = false) String q,
            Pageable pageable) {

        Specification<Employee> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("fullName")), "%" + q.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("username")), "%" + q.toLowerCase() + "%")
                    ));
        }

        return ResponseEntity.ok(employeeService.getAllEmployees(spec, pageable));
    }

    @Operation(summary = "Получить сотрудника по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Employee> getEmployee(@Parameter(description = "ID сотрудника") @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @Operation(summary = "Создать нового сотрудника")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        return new ResponseEntity<>(employeeService.createEmployee(employee), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить сотрудника")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employee> updateEmployee(
            @Parameter(description = "ID сотрудника") @PathVariable Long id,
            @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, employee));
    }

    @Operation(summary = "Синхронизировать текущего пользователя из Keycloak")
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> syncCurrentEmployee(@AuthenticationPrincipal Jwt jwt) {
        Employee synced = employeeService.syncOrCreateFromKeycloak(jwt);
        return ResponseEntity.ok(synced);
    }

    @Operation(summary = "Удалить сотрудника")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmployee(@Parameter(description = "ID сотрудника") @PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
