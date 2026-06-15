package com.example.calculatorservice.repository;

import com.example.calculatorservice.entity.Eyelet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EyeletRepository extends JpaRepository<Eyelet, Long> {
}
