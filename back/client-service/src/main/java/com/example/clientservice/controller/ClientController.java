package com.example.clientservice.controller;

import com.example.common.entity.Client;
import com.example.clientservice.service.ClientService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Service", description = "API для управления клиентами")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Получить список клиентов")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Page<Client>> getAllClients(
            @Parameter(description = "Поисковый запрос") @RequestParam(required = false) String q,
            Pageable pageable) {

        Specification<Client> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("contactPerson")), "%" + q.toLowerCase() + "%")
                    ));
        }

        return ResponseEntity.ok(clientService.getAllClients(spec, pageable));
    }

    @Operation(summary = "Найти клиентов по запросу")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<Client>> searchClients(
            @RequestParam String q) {
        return ResponseEntity.ok(clientService.searchClients(q));
    }

    @Operation(summary = "Получить клиента по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Client> getClient(@Parameter(description = "ID клиента") @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @Operation(summary = "Создать нового клиента")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        return new ResponseEntity<>(clientService.createClient(client), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить клиента")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Client> updateClient(
            @Parameter(description = "ID клиента") @PathVariable Long id,
            @RequestBody Client client) {
        return ResponseEntity.ok(clientService.updateClient(id, client));
    }

    @Operation(summary = "Удалить клиента")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteClient(@Parameter(description = "ID клиента") @PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
