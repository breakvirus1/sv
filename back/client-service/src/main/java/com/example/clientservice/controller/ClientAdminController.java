package com.example.clientservice.controller;

import com.example.common.entity.Client;
import com.example.clientservice.service.ClientAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/clients")
@RequiredArgsConstructor
@Tag(name = "Client Admin", description = "Админ API для управления клиентами")
public class ClientAdminController {

    private final ClientAdminService clientAdminService;

    @Operation(summary = "Сгенерировать 20 тестовых клиентов")
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Client>> generateTestClients() {
        List<Client> clients = clientAdminService.generateTestClients(20);
        return ResponseEntity.ok(clients);
    }
}
