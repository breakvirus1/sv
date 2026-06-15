package com.example.orderservice.repository;

import com.example.orderservice.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    Optional<FileAttachment> findByOrderItemId(Long orderItemId);
}
