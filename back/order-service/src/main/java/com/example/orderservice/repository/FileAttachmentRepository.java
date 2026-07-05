package com.example.orderservice.repository;

import com.example.orderservice.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    Optional<FileAttachment> findByOrderItemId(Long orderItemId);

    List<FileAttachment> findByOrderItem_IdIn(List<Long> orderItemIds);
}
