package com.example.clientservice.controller;

import com.example.clientservice.dto.ClientCreateRequest;
import com.example.clientservice.dto.ClientResponse;
import com.example.clientservice.dto.ClientUpdateRequest;
import com.example.clientservice.entity.Client;
import com.example.clientservice.service.ClientService;
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

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Service", description = "API для управления клиентами")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Получить список клиентов")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<Page<ClientResponse>> getAllClients(
            @Parameter(description = "Поисковый запрос") @RequestParam(required = false) String q,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        Specification<Client> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("contactPerson")), "%" + q.toLowerCase() + "%")
                    ));
        }

        return ResponseEntity.ok(clientService.getAllClients(spec, pageable, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Найти клиентов по запросу")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<List<Client>> searchClients(
            @RequestParam String q,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientService.searchClients(q, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Получить клиента по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<ClientResponse> getClient(@Parameter(description = "ID клиента") @PathVariable Long id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientService.getClientById(id, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Создать нового клиента")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (isGod(jwt)) {
            if (request.getCompanyId() == null) {
                request.setCompanyId(extractCompanyId(jwt));
            }
        } else {
            request.setCompanyId(requireCompanyId(jwt));
        }
        return new ResponseEntity<>(clientService.createClient(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить клиента")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<ClientResponse> updateClient(
            @Parameter(description = "ID клиента") @PathVariable Long id,
            @Valid @RequestBody ClientUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientService.updateClient(id, request, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Удалить клиента")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<Void> deleteClient(@Parameter(description = "ID клиента") @PathVariable Long id,
                                             @AuthenticationPrincipal Jwt jwt) {
        clientService.deleteClient(id, scopedCompanyId(jwt));
        return ResponseEntity.noContent().build();
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
