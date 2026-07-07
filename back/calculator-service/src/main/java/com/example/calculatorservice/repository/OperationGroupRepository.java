package com.example.calculatorservice.repository;

import com.example.calculatorservice.entity.OperationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperationGroupRepository extends JpaRepository<OperationGroup, Long> {
    List<OperationGroup> findAll();
    Optional<OperationGroup> findByName(String name);
}
