package com.example.commentservice.repository;

import com.example.commentservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByOrderIdAndDeletedFalse(Long orderId);
}
