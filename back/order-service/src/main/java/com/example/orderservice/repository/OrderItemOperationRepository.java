package com.example.orderservice.repository;

import com.example.orderservice.order.entity.OrderItemOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemOperationRepository extends JpaRepository<OrderItemOperation, Long> {
}
