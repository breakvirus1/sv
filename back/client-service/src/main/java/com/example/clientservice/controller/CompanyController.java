package com.example.clientservice.controller;

import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.service.CompanyService;
import com.example.printsv.common.tenant.TenantContext;
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

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Company Service", description = "API для управления компаниями")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Получить список компаний")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<Page<CompanyResponse>> getAllCompanies(
            @Parameter(description = "Поисковый запрос") @RequestParam(required = false) String q,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        Specification<com.example.clientservice.entity.Company> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%")
            );
        }
        final Specification<com.example.clientservice.entity.Company> finalSpec = spec;

        return ResponseEntity.ok(TenantContext.runAs(null,
                () -> companyService.getAllCompanies(finalSpec, pageable, extractCompanyId(jwt))));
    }

    @Operation(summary = "Получить компанию по ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponse> getCompany(
            @Parameter(description = "ID компании") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        // companies — глобальный справочник без company_id; читаем из мастер-БД,
        // иначе запрос уходит в БД компании (tenant), где таблица пуста.
        Long requesterCompanyId = extractCompanyId(jwt);
        CompanyResponse response = TenantContext.runAs(null,
                () -> companyService.getCompanyById(id, requesterCompanyId));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Создать новую компанию")
    @PostMapping
    @PreAuthorize("hasRole('GOD')")
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        return new ResponseEntity<>(companyService.createCompany(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить компанию")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<CompanyResponse> updateCompany(
            @Parameter(description = "ID компании") @PathVariable Long id,
            @Valid @RequestBody CompanyCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(companyService.updateCompany(id, request, extractCompanyId(jwt)));
    }

    @Operation(summary = "Удалить компанию")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<Void> deleteCompany(
            @Parameter(description = "ID компании") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        companyService.deleteCompany(id, extractCompanyId(jwt));
        return ResponseEntity.noContent().build();
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
