package com.example.orderservice.repository;

import com.example.common.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByPaymentDateDesc(Long orderId);
}
