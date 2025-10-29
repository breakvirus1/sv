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

@RestController
@RequestMapping("/api/subzakaz")
public class SubZakazController {
    @Autowired
    private SubZakazService subZakazService;

    @PostMapping("/{zakazId}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<SubZakaz> createSubZakaz(@PathVariable Long zakazId, @RequestBody SubZakaz subZakaz) {
        return ResponseEntity.ok(subZakazService.addSubZakaz(zakazId, subZakaz));
    }

    @GetMapping("/{zakazId}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<List<SubZakaz>> getAllSubZakazByZakazId(@PathVariable Long zakazId) {
        return ResponseEntity.ok(subZakazService.getAllSubZakazByZakazId(zakazId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<SubZakaz> updateSubZakaz(@PathVariable Long id, @RequestBody SubZakaz subZakazDetails) {
        return ResponseEntity.ok(subZakazService.updateSubZakaz(id, subZakazDetails));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<Void> deleteSubZakaz(@PathVariable Long id) {
        subZakazService.deleteSubZakaz(id);
        return ResponseEntity.noContent().build();
    }
}