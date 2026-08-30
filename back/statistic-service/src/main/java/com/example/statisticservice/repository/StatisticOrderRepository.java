package com.example.statisticservice.repository;

import com.example.statisticservice.entity.StatisticOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StatisticOrderRepository extends JpaRepository<StatisticOrder, Long>, JpaSpecificationExecutor<StatisticOrder> {
    Optional<StatisticOrder> findByOrderId(Long orderId);
}
