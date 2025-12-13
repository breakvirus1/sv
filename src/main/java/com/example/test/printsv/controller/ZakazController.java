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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.printsv.request.ZakazRequest;

import com.example.test.printsv.response.ZakazResponse;
import com.example.test.printsv.service.ZakazService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
public class ZakazController {
    @Autowired
    private ZakazService zakazService;

    @Operation(summary = "Получить список заказов для пользователя по id", description = "Возвращает список заказов (только для оператора)")
    @GetMapping("/zakaz/all")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<List<ZakazResponse>> getAllZakazByUser(@RequestParam("id") Long id) {
        return ResponseEntity.ok(zakazService.getAllZakazByUserId(id));
    }

    @Operation(summary = "создать заказ с введенной суммой")
    @PostMapping("/zakaz")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<ZakazResponse> createZakaz(@RequestBody ZakazRequest request) {
        return ResponseEntity.ok(zakazService.addZakaz(request));
    }
    
    @Operation(summary = "получить заказ по id")
    @GetMapping("/zakaz/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<ZakazResponse> getZakazById(@PathVariable Long id) {
        return ResponseEntity.ok(zakazService.getZakazById(id));
    }

    @Operation(summary = "обновить заказ")
    @PutMapping("/zakaz/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<ZakazResponse> updateZakaz(@PathVariable Long id) {
        return ResponseEntity.ok(zakazService.updateZakaz(id));
    }

    @Operation(summary = "удалить заказ по id")
    @DeleteMapping("/zakaz/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<Void> deleteZakaz(@PathVariable Long id) {
        zakazService.deleteZakaz(id);
        return ResponseEntity.noContent().build();
    }
}
