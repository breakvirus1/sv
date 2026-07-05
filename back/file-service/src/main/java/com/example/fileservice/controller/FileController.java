package com.example.fileservice.controller;

import com.example.fileservice.entity.FileAttachment;
import com.example.fileservice.repository.FileAttachmentRepository;
import com.example.fileservice.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            @RequestParam(value = "orderNumber", required = false) String orderNumber,
            @RequestParam(value = "managerName", required = false) String managerName,
            @RequestParam(value = "clientName", required = false) String clientName,
            @RequestParam(value = "materialName", required = false) String materialName,
            @RequestParam(value = "operationNames", required = false) String operationNames,
            @RequestParam(value = "operationParams", required = false) String operationParams) {

        FileAttachment attachment = fileService.storeFile(file, orderId, orderItemId, uploadedBy, orderNumber, managerName, clientName, materialName, operationNames, operationParams);
        FileAttachment saved = fileAttachmentRepository.save(attachment);

        if (orderItemId != null) {
            jdbcTemplate.update("UPDATE svschema.order_items SET file_id = ? WHERE id = ?", saved.getId(), orderItemId);
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

    @GetMapping("/show/{fileName:.+}")
    public ResponseEntity<Map<String, String>> showInFolder(@PathVariable String fileName) {
        Path filePath = fileService.getFilePath(fileName);
        try {
            if (!Desktop.isDesktopSupported()) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("parentDir", filePath.getParent().toString()));
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("parentDir", filePath.getParent().toString()));
            }
            Path parentDir = filePath.getParent();
            if (parentDir != null && Files.exists(parentDir)) {
                desktop.open(parentDir.toFile());
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Collections.singletonMap("status", "opened"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Collections.singletonMap("error", "File not found"));
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
            jdbcTemplate.update("UPDATE svschema.order_items SET file_id = NULL WHERE id = ?", file.getOrderItem().getId());
        }
        fileService.deleteFile(file);
        fileAttachmentRepository.delete(file);
        return ResponseEntity.ok().build();
    }
}
