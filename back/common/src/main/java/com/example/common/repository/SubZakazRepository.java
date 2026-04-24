package com.example.common.repository;

import com.example.common.entity.SubZakaz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubZakazRepository extends JpaRepository<SubZakaz, Long> {
}
