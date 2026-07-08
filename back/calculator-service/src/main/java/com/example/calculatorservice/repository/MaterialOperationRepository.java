package com.example.calculatorservice.repository;

import com.example.calculatorservice.entity.MaterialOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialOperationRepository extends JpaRepository<MaterialOperation, Long> {
    List<MaterialOperation> findByMaterialIdAndDeletedFalse(Long materialId);
    List<MaterialOperation> findByOperationIdAndDeletedFalse(Long operationId);
    void deleteByMaterialIdAndDeletedFalse(Long materialId);
}
