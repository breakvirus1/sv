package com.example.test.printsv.repository;

import com.example.test.printsv.entity.Zakaz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ZakazRepository extends JpaRepository<Zakaz, Long> {
    List<Zakaz> findByUserOfZakazId(Long userId);
    
    List<Zakaz> findByCustomerOfZakazNameIgnoreCase(String customerName);
}