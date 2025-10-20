package com.example.test.printsv.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.test.printsv.request.ListZakazByUserIdRequest;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ListZakazByUserIdResponse;
import com.example.test.printsv.response.ZakazResponse;
import com.example.test.printsv.service.ZakazService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@RestController
@AllArgsConstructor

@RequestMapping("/api/users")
public class ZakazController {
    @Autowired
    private ZakazService zakazService;


    @GetMapping("/zakaz/all")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<ListZakazByUserIdResponse> getAllZakazByUser(@RequestParam("id") Long id) {

        return ResponseEntity.ok(zakazService.getAllZakazByUserId(id));
    }


    @PostMapping("/zakaz")
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ResponseEntity<ZakazResponse> createZakaz(@RequestBody ZakazRequest request) {
        return ResponseEntity.ok(zakazService.addZakaz(request.getSum()));
    }
    
}