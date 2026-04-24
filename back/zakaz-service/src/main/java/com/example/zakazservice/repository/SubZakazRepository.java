package com.example.zakazservice.repository;

import com.example.common.entity.SubZakaz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubZakazRepository extends JpaRepository<SubZakaz, Long> {
    List<SubZakaz> findAllByZakazId(Long zakazId);
}
