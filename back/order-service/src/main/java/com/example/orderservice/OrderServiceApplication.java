package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
    "com.example.orderservice.entity",
    "com.example.orderservice.product",
    "com.example.orderservice.order.entity",
    "com.example.clientservice.entity",
    "com.example.employeeservice.entity",
    "com.example.materialservice.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.example.orderservice.repository",
    "com.example.orderservice.product.repository",
    "com.example.materialservice.repository"
})
@EnableJpaAuditing
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
