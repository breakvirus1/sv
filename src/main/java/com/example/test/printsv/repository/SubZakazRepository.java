package com.example.test.printsv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.test.printsv.entity.SubZakaz;

public interface SubZakazRepository extends JpaRepository<SubZakaz,Long> {
    List<SubZakaz> findAllByZakazId(Long zakazId);

}
