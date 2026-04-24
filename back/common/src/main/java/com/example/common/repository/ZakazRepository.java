package com.example.common.repository;

import com.example.common.entity.Zakaz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZakazRepository extends JpaRepository<Zakaz, Long> {
    List<Zakaz> findByUserId(Long userId);
}
