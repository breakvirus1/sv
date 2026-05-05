package com.example.orderservice.product.repository;

import com.example.orderservice.product.ProductMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, Long> {
}
