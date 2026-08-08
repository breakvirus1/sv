package com.example.commentservice.repository;

import com.example.commentservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndReadedFalseAndDeletedFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadedFalseAndDeletedFalse(Long userId);
}
