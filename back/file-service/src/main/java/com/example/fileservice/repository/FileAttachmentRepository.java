package com.example.fileservice.repository;

import com.example.fileservice.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    Optional<FileAttachment> findByOrderItemId(Long orderItemId);
    List<FileAttachment> findByOrderId(Long orderId);
}
