package com.example.statisticservice.repository;

import com.example.statisticservice.entity.StatisticOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StatisticOrderItemRepository extends JpaRepository<StatisticOrderItem, Long>, JpaSpecificationExecutor<StatisticOrderItem> {
    Optional<StatisticOrderItem> findByOrderItemId(Long orderItemId);
    List<StatisticOrderItem> findByStatisticOrderId(Long statisticOrderId);
}
