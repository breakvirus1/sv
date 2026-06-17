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
    public ResponseEntity<List<Client>> generateClients(@PathVariable int count) {
        return ResponseEntity.ok(generateDataService.generateClients(count));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовых сотрудников")
    @PostMapping("/employees/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Employee>> generateEmployees(@PathVariable int count) {
        return ResponseEntity.ok(generateDataService.generateEmployees(count));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые материалы")
    @PostMapping("/materials/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Material>> generateMaterials(@PathVariable int count) {
        return ResponseEntity.ok(generateDataService.generateMaterials(count));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые операции")
    @PostMapping("/operations/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.example.generatedataservice.entity.Operation>> generateOperations(@PathVariable int count) {
        return ResponseEntity.ok(generateDataService.generateOperations(count));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые цеха")
    @PostMapping("/workshops/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Workshop>> generateWorkshops(@PathVariable int count) {
        return ResponseEntity.ok(generateDataService.generateWorkshops(count));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать тестовые заказы")
    @PostMapping("/orders/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> generateOrders(@PathVariable int count) {
        return ResponseEntity.ok(generateDataService.generateOrders(count));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Сгенерировать данные во всех таблицах")
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateAll(@RequestBody Map<String, Integer> counts) {
        int clients = counts.getOrDefault("clients", 0);
        int materials = counts.getOrDefault("materials", 0);
        int operations = counts.getOrDefault("operations", 0);
        int workshops = counts.getOrDefault("workshops", 0);
        int employees = counts.getOrDefault("employees", 0);
        int orders = counts.getOrDefault("orders", 0);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (clients > 0) result.put("clients", generateDataService.generateClients(clients));
        if (materials > 0) result.put("materials", generateDataService.generateMaterials(materials));
        if (operations > 0) result.put("operations", generateDataService.generateOperations(operations));
        if (workshops > 0) result.put("workshops", generateDataService.generateWorkshops(workshops));
        if (employees > 0) result.put("employees", generateDataService.generateEmployees(employees));
        if (orders > 0) result.put("orders", generateDataService.generateOrders(orders));

        return ResponseEntity.ok(result);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить всех клиентов")
    @DeleteMapping("/clients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllClients() {
        int deleted = generateDataService.deleteAllClients();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить всех сотрудников")
    @DeleteMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllEmployees() {
        int deleted = generateDataService.deleteAllEmployees();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все материалы")
    @DeleteMapping("/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllMaterials() {
        int deleted = generateDataService.deleteAllMaterials();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все операции")
    @DeleteMapping("/operations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllOperations() {
        int deleted = generateDataService.deleteAllOperations();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все цеха")
    @DeleteMapping("/workshops")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllWorkshops() {
        int deleted = generateDataService.deleteAllWorkshops();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить все заказы")
    @DeleteMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteAllOrders() {
        int deleted = generateDataService.deleteAllOrders();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
