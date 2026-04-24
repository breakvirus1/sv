package com.example.zakazservice.controller;

import java.util.List;

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

import com.example.common.dto.ZakazDto;
import com.example.zakazservice.response.ZakazResponse;
import com.example.zakazservice.service.ZakazService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/zakaz")
public class ZakazController {

    private final ZakazService zakazService;

    @GetMapping("/all")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<List<ZakazResponse>> getAllByUserId(@RequestParam Long userId) {
        return ResponseEntity.ok(zakazService.getAllZakazByUserId(userId));
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ZakazResponse> createZakaz(@RequestBody ZakazDto zakazDto) {
        return ResponseEntity.ok(zakazService.addZakaz(zakazDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ZakazResponse> getZakazById(@PathVariable Long id) {
        return ResponseEntity.ok(zakazService.getZakazById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ZakazResponse> updateZakaz(@PathVariable Long id) {
        return ResponseEntity.ok(zakazService.updateZakaz(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Void> deleteZakaz(@PathVariable Long id) {
        zakazService.deleteZakaz(id);
        return ResponseEntity.noContent().build();
    }
}
