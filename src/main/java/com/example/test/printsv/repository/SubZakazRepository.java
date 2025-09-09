package com.example.test.printsv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.Zakaz;


import java.util.List;

@Repository
public interface SubZakazRepository extends JpaRepository<SubZakaz, Long> {
List<SubZakaz> findByNameContaining(String name);
    List<SubZakaz> findByDone(Boolean done);
}
