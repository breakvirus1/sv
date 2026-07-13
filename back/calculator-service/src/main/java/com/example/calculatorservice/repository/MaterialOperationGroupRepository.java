package com.example.calculatorservice.repository;

import com.example.calculatorservice.entity.MaterialOperationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialOperationGroupRepository extends JpaRepository<MaterialOperationGroup, Long> {

    List<MaterialOperationGroup> findByMaterialIdAndDeletedFalse(Long materialId);

    @Query("SELECT mog FROM MaterialOperationGroup mog WHERE mog.materialId = :materialId AND mog.deleted = false")
    List<MaterialOperationGroup> findAllByMaterialId(@Param("materialId") Long materialId);

    @Modifying
    @Query("DELETE FROM MaterialOperationGroup mog WHERE mog.materialId = :materialId AND mog.deleted = false")
    int deleteByMaterialId(@Param("materialId") Long materialId);

    boolean existsByMaterialIdAndOperationGroupIdAndOperationIdAndDeletedFalse(
            Long materialId, Long operationGroupId, Long operationId);

    @Modifying
    @Query(value = "DELETE FROM svschema.material_operation_groups WHERE material_id = ?1", nativeQuery = true)
    int hardDeleteByMaterialId(Long materialId);
}
