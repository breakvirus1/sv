package com.example.orderservice.repository;

import com.example.common.entity.OrderStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStageRepository extends JpaRepository<OrderStage, Long> {
    List<OrderStage> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
