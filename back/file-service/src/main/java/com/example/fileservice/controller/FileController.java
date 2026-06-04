package com.example.fileservice.controller;

import com.example.fileservice.entity.FileAttachment;
import com.example.fileservice.repository.FileAttachmentRepository;
import com.example.fileservice.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final JdbcTemplate jdbcTemplate;

    public FileController(FileService fileService, FileAttachmentRepository fileAttachmentRepository, JdbcTemplate jdbcTemplate) {
        this.fileService = fileService;
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileAttachment> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "orderItemId", required = false) Long orderItemId,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {

        FileAttachment attachment = fileService.storeFile(file, orderId, orderItemId, uploadedBy);
        FileAttachment saved = fileAttachmentRepository.save(attachment);

        if (orderItemId != null) {
            jdbcTemplate.update("UPDATE ordschema.order_items SET file_id = ? WHERE id = ?", saved.getId(), orderItemId);
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Path filePath = fileService.getFilePath(fileName);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<FileAttachment> getByOrderItem(@PathVariable Long orderItemId) {
        return fileAttachmentRepository.findByOrderItemId(orderItemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        FileAttachment file = fileAttachmentRepository.findById(fileId).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        if (file.getOrderItem() != null) {
            jdbcTemplate.update("UPDATE ordschema.order_items SET file_id = NULL WHERE id = ?", file.getOrderItem().getId());
        }
        fileService.deleteFile(file);
        fileAttachmentRepository.delete(file);
        return ResponseEntity.ok().build();
    }
}
