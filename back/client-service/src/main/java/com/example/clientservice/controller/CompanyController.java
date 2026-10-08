package com.example.clientservice.controller;

import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.service.CompanyService;
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
            Pageable pageable) {

        Specification<com.example.clientservice.entity.Company> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%")
            );
        }

        return ResponseEntity.ok(companyService.getAllCompanies(spec, pageable));
    }

    @Operation(summary = "Получить компанию по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<CompanyResponse> getCompany(
            @Parameter(description = "ID компании") @PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @Operation(summary = "Создать новую компанию")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<CompanyResponse> createCompany(@RequestBody CompanyCreateRequest request) {
        return new ResponseEntity<>(companyService.createCompany(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить компанию")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<CompanyResponse> updateCompany(
            @Parameter(description = "ID компании") @PathVariable Long id,
            @RequestBody CompanyCreateRequest request) {
        return ResponseEntity.ok(companyService.updateCompany(id, request));
    }

    @Operation(summary = "Удалить компанию")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<Void> deleteCompany(
            @Parameter(description = "ID компании") @PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}
