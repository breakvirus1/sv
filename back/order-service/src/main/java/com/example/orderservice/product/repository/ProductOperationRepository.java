package com.example.orderservice.product.repository;

import com.example.orderservice.product.ProductOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOperationRepository extends JpaRepository<ProductOperation, Long> {
}
