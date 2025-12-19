package com.example.test.printsv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.test.printsv.entity.SubZakaz;

public interface SubZakazRepository extends JpaRepository<SubZakaz,Long> {
    @Query("SELECT s FROM SubZakaz s WHERE s.zakaz.id = :zakazId")
    List<SubZakaz> findAllByZakazId(Long zakazId);

}
