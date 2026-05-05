package com.example.orderservice.repository;

import com.example.orderservice.order.entity.OrderItemMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemMaterialRepository extends JpaRepository<OrderItemMaterial, Long> {
}
