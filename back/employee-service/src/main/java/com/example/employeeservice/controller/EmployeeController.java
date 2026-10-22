package com.example.employeeservice.controller;

import com.example.employeeservice.dto.EmployeeCreateRequest;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.dto.EmployeeUpdateRequest;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * REST Controller для управления сотрудниками.
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Service", description = "API для управления сотрудниками")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Получить список сотрудников")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'GOD')")
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long companyId,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        Specification<Employee> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("fullName")), "%" + q.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("username")), "%" + q.toLowerCase() + "%")
                    ));
        }

        Long effectiveCompanyId;
        if (isGod(jwt)) {
            effectiveCompanyId = companyId != null ? companyId : extractCompanyId(jwt);
        } else {
            effectiveCompanyId = requireCompanyId(jwt);
        }
        return ResponseEntity.ok(employeeService.getAllEmployees(spec, pageable, effectiveCompanyId));
    }

    @Operation(summary = "Получить сотрудника по username")
    @GetMapping("/username/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> getEmployeeByUsername(@PathVariable String username,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(employeeService.getEmployeeByUsername(username, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Получить сотрудника по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<EmployeeResponse> getEmployee(@Parameter(description = "ID сотрудника") @PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Создать нового сотрудника")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        if (isGod(jwt)) {
            if (request.getCompanyId() == null) {
                request.setCompanyId(extractCompanyId(jwt));
            }
        } else {
            request.setCompanyId(requireCompanyId(jwt));
        }
        return new ResponseEntity<>(employeeService.createEmployee(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить сотрудника")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @Parameter(description = "ID сотрудника") @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Синхронизировать текущего пользователя из Keycloak")
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> syncCurrentEmployee(@AuthenticationPrincipal Jwt jwt) {
        EmployeeResponse synced = employeeService.syncOrCreateFromKeycloak(jwt);
        return ResponseEntity.ok(synced);
    }

    @Operation(summary = "Синхронизировать всех пользователей из Keycloak (ADMIN)")
    @PostMapping("/sync-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<String> syncAllFromKeycloak(@AuthenticationPrincipal Jwt jwt) {
        Long companyId = extractCompanyId(jwt);
        // GOD role can sync all, ADMIN role syncs only their company
        List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles != null && roles.contains("GOD")) {
            companyId = null;
        }
        String result = employeeService.syncFromKeycloakByCompany(companyId);
        return ResponseEntity.ok("Синхронизация завершена. " + result);
    }

    @Operation(summary = "Синхронизировать пользователей из Keycloak по компании (ADMIN компании)")
    @PostMapping("/sync-by-company")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<String> syncByCompany(@RequestParam Long companyId,
                                                @AuthenticationPrincipal Jwt jwt) {
        Long effectiveCompanyId = companyId;
        if (jwt != null && !isGod(jwt)) {
            effectiveCompanyId = requireCompanyId(jwt);
            if (!effectiveCompanyId.equals(companyId)) {
                throw new org.springframework.security.access.AccessDeniedException("Cannot sync another company");
            }
        }
        String result = employeeService.syncFromKeycloakByCompany(effectiveCompanyId);
        return ResponseEntity.ok("Синхронизация по компании завершена. " + result);
    }

    @Operation(summary = "Удалить сотрудника")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<Void> deleteEmployee(@Parameter(description = "ID сотрудника") @PathVariable Long id,
                                               @AuthenticationPrincipal Jwt jwt) {
        employeeService.deleteEmployee(id, scopedCompanyId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить роли сотрудника из Keycloak по username")
    @GetMapping("/roles/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<List<String>> getEmployeeRoles(@PathVariable String username,
                                                         @AuthenticationPrincipal Jwt jwt) {
        employeeService.getEmployeeByUsername(username, scopedCompanyId(jwt));
        return ResponseEntity.ok(employeeService.getKeycloakRolesForUser(username));
    }

    private Long scopedCompanyId(Jwt jwt) {
        if (jwt == null) return null;
        if (isGod(jwt)) return extractCompanyId(jwt);
        return requireCompanyId(jwt);
    }

    private Long requireCompanyId(Jwt jwt) {
        Long companyId = extractCompanyId(jwt);
        if (companyId == null && jwt != null) {
            throw new org.springframework.security.access.AccessDeniedException("company_id claim is required");
        }
        return companyId;
    }

    private boolean isGod(Jwt jwt) {
        if (jwt == null) return false;
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> access) {
            Object roles = access.get("roles");
            if (roles instanceof java.util.List<?> list) {
                return list.stream().anyMatch(r -> "GOD".equals(String.valueOf(r)) || "ROLE_GOD".equals(String.valueOf(r)));
            }
        }
        return false;
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
