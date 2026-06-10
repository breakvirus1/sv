package com.example.materialservice.repository;

import com.example.materialservice.entity.MaterialOperation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialOperationRepository extends JpaRepository<MaterialOperation, Long> {
    List<MaterialOperation> findByMaterialIdOrderBySortOrderAsc(Long materialId);
    List<MaterialOperation> findByMaterialIdAndActiveTrueOrderBySortOrderAsc(Long materialId);
    void deleteByMaterialId(Long materialId);
}
