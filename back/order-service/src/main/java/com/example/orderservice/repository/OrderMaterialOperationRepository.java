package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderMaterialOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderMaterialOperationRepository extends JpaRepository<OrderMaterialOperation, Long> {
}
