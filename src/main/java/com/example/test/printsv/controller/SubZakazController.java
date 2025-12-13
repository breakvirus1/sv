package com.example.test.printsv.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.service.SubZakazService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/subzakaz")
public class SubZakazController {
    @Autowired
    private SubZakazService subZakazService;

    @Operation(summary = "Создать позицию для заказа", description = "Создает новую позицию для заказа ")
    @PostMapping("/{zakazId}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<SubZakaz> createSubZakaz(@PathVariable Long zakazId, @RequestBody SubZakaz subZakaz) {
        return ResponseEntity.ok(subZakazService.addSubZakaz(zakazId, subZakaz));
    }

    @Operation(summary = "Получить список позиций для заказа", description = "Возвращает список позиций для заказа (только для оператора)")

    @GetMapping("/{zakazId}/subzakaz")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<List<SubZakaz>> getAllSubZakazByZakazId(@PathVariable Long zakazId) {
        return ResponseEntity.ok(subZakazService.getAllSubZakazByZakazId(zakazId));
    }

    @Operation(summary = "Обновить позицию заказа", description = "Обновляет позицию заказа (только для оператора)")
    @PutMapping("/{zakazId}/subzakaz/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<SubZakaz> updateSubZakaz(@PathVariable Long zakazId, @PathVariable Long id, @RequestBody SubZakaz subZakazDetails) {
        return ResponseEntity.ok(subZakazService.updateSubZakaz(id, subZakazDetails));
    }

    @Operation(summary = "Удалить позицию заказа", description = "Удаляет позицию заказа (только для оператора)")
    @DeleteMapping("/{zakazId}/subzakaz/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<Void> deleteSubZakaz(@PathVariable Long id) {
        subZakazService.deleteSubZakaz(id);
        return ResponseEntity.noContent().build();
    }
}