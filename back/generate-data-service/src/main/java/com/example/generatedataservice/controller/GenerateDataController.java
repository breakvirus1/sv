package com.example.generatedataservice.controller;

import com.example.generatedataservice.entity.Client;
import com.example.generatedataservice.entity.Employee;
import com.example.generatedataservice.entity.Material;
import com.example.generatedataservice.entity.Order;
import com.example.generatedataservice.entity.Workshop;
import com.example.generatedataservice.service.GenerateDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/generate")
@RequiredArgsConstructor
@Tag(name = "Generate Data", description = "API для генерации тестовых данных")
public class GenerateDataController {

    private final GenerateDataService generateDataService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовых клиентов")
    @PostMapping("/clients/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Client>> generateClients(@PathVariable int count, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateClients(count, extractCompanyId(jwt)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовых сотрудников")
    @PostMapping("/employees/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Employee>> generateEmployees(@PathVariable int count, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateEmployees(count, extractCompanyId(jwt)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые материалы")
    @PostMapping("/materials/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Material>> generateMaterials(@PathVariable int count, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateMaterials(count, extractCompanyId(jwt)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые операции")
    @PostMapping("/operations/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.example.generatedataservice.entity.Operation>> generateOperations(@PathVariable int count, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateOperations(count, extractCompanyId(jwt)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые цеха")
    @PostMapping("/workshops/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Workshop>> generateWorkshops(@PathVariable int count, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateWorkshops(count, extractCompanyId(jwt)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые заказы")
    @PostMapping("/orders/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> generateOrders(@PathVariable int count, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateOrders(count, extractCompanyId(jwt)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать данные во всех таблицах")
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateAll(@RequestBody Map<String, Integer> counts, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(generateDataService.generateAll(counts, extractCompanyId(jwt)));
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

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить всех клиентов")
    @DeleteMapping("/clients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllClients(@AuthenticationPrincipal Jwt jwt) {
        int deleted = generateDataService.deleteAllClients(extractCompanyId(jwt));
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить всех сотрудников")
    @DeleteMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllEmployees(@AuthenticationPrincipal Jwt jwt) {
        int deleted = generateDataService.deleteAllEmployees(extractCompanyId(jwt));
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все материалы")
    @DeleteMapping("/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllMaterials(@AuthenticationPrincipal Jwt jwt) {
        int deleted = generateDataService.deleteAllMaterials(extractCompanyId(jwt));
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все операции")
    @DeleteMapping("/operations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllOperations(@AuthenticationPrincipal Jwt jwt) {
        int deleted = generateDataService.deleteAllOperations(extractCompanyId(jwt));
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все цеха")
    @DeleteMapping("/workshops")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllWorkshops(@AuthenticationPrincipal Jwt jwt) {
        int deleted = generateDataService.deleteAllWorkshops(extractCompanyId(jwt));
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все заказы")
    @DeleteMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllOrders(@AuthenticationPrincipal Jwt jwt) {
        int deleted = generateDataService.deleteAllOrders(extractCompanyId(jwt));
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
