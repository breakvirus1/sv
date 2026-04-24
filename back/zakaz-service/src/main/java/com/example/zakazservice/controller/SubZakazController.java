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
import org.springframework.web.bind.annotation.RestController;

import com.example.common.dto.SubZakazDto;
import com.example.zakazservice.response.SubZakazResponse;
import com.example.zakazservice.service.SubZakazService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subzakaz")
public class SubZakazController {

    private final SubZakazService subZakazService;

    @PostMapping("/{zakazId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<SubZakazResponse> createSubZakaz(@PathVariable Long zakazId, @RequestBody SubZakazDto subZakazDto) {
        return ResponseEntity.ok(subZakazService.addSubZakaz(zakazId, subZakazDto));
    }

    @GetMapping("/zakaz/{zakazId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<List<SubZakazResponse>> getAllSubZakazByZakazId(@PathVariable Long zakazId) {
        return ResponseEntity.ok(subZakazService.getAllByZakazId(zakazId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<SubZakazResponse> updateSubZakaz(@PathVariable Long id, @RequestBody SubZakazDto subZakazDto) {
        return ResponseEntity.ok(subZakazService.updateSubZakaz(id, subZakazDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Void> deleteSubZakaz(@PathVariable Long id) {
        subZakazService.deleteSubZakaz(id);
        return ResponseEntity.noContent().build();
    }
}
