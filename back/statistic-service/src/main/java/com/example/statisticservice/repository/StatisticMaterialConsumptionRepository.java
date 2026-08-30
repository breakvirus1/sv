package com.example.statisticservice.repository;

import com.example.statisticservice.entity.StatisticMaterialConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StatisticMaterialConsumptionRepository extends JpaRepository<StatisticMaterialConsumption, Long>, JpaSpecificationExecutor<StatisticMaterialConsumption> {
    Optional<StatisticMaterialConsumption> findByOrderMaterialId(Long orderMaterialId);
    List<StatisticMaterialConsumption> findByStatisticOrderItemId(Long statisticOrderItemId);
}
