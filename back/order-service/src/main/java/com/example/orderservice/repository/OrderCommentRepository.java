package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderCommentRepository extends JpaRepository<OrderComment, Long> {
    List<OrderComment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
