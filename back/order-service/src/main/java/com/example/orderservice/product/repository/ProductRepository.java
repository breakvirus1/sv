package com.example.orderservice.product.repository;

import com.example.orderservice.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
