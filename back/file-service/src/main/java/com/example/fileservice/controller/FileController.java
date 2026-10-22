package com.example.fileservice.controller;

import com.example.fileservice.entity.FileAttachment;
import com.example.fileservice.repository.FileAttachmentRepository;
import com.example.fileservice.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Desktop;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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

        Long companyId = requireCompanyId();
        boolean god = isGod();

        if (orderId != null) {
            assertEntityAccess("orders", orderId, companyId, god);
        }
        if (orderItemId != null) {
            assertEntityAccess("order_items", orderItemId, companyId, god);
        }

        FileAttachment attachment = fileService.storeFile(file, orderId, orderItemId, uploadedBy, orderNumber, managerName, clientName, materialName, operationNames, operationParams);
        attachment.setCompanyId(companyId);
        FileAttachment saved = fileAttachmentRepository.save(attachment);

        if (orderItemId != null) {
            jdbcTemplate.update("UPDATE svschema.order_items SET file_id = ? WHERE id = ?", saved.getId(), orderItemId);
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        FileAttachment attachment = findAccessibleAttachment(fileName);
        Path filePath = fileService.getFilePath(attachment.getFileName());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
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
        FileAttachment attachment = findAccessibleAttachment(fileName);
        Path filePath = fileService.getFilePath(attachment.getFileName());
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
        Long companyId = requireCompanyId();
        boolean god = isGod();
        assertEntityAccess("order_items", orderItemId, companyId, god);

        return fileAttachmentRepository.findByOrderItemId(orderItemId)
                .filter(f -> god || companyId.equals(f.getCompanyId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        FileAttachment file = fileAttachmentRepository.findById(fileId).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        assertAccess(file.getCompanyId(), requireCompanyId(), isGod());

        if (file.getOrderItem() != null) {
            jdbcTemplate.update("UPDATE svschema.order_items SET file_id = NULL WHERE id = ?", file.getOrderItem().getId());
        }
        fileService.deleteFile(file);
        fileAttachmentRepository.delete(file);
        return ResponseEntity.ok().build();
    }

    private FileAttachment findAccessibleAttachment(String fileName) {
        Long companyId = requireCompanyId();
        boolean god = isGod();
        List<FileAttachment> matches = fileAttachmentRepository.findAllByFileNameOrderByIdDesc(fileName);
        return matches.stream()
                .filter(f -> god || companyId.equals(f.getCompanyId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private void assertEntityAccess(String table, Long entityId, Long companyId, boolean god) {
        Long entityCompanyId;
        try {
            entityCompanyId = jdbcTemplate.queryForObject(
                    "SELECT company_id FROM svschema." + table + " WHERE id = ?", Long.class, entityId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found: " + entityId);
        }
        if (!god) {
            assertAccess(entityCompanyId, companyId, false);
        }
    }

    private void assertAccess(Long entityCompanyId, Long companyId, boolean god) {
        if (god) {
            return;
        }
        if (entityCompanyId == null || !entityCompanyId.equals(companyId)) {
            throw new AccessDeniedException("File belongs to another company");
        }
    }

    private Long requireCompanyId() {
        Long companyId = extractCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("company_id claim is required");
        }
        return companyId;
    }

    private boolean isGod() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_GOD".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long extractCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            try {
                Object claim = jwt.getClaim("company_id");
                if (claim instanceof Number number) {
                    return number.longValue();
                }
                if (claim instanceof String str) {
                    return Long.parseLong(str);
                }
                if (claim instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Number number) return number.longValue();
                    if (first instanceof String str) return Long.parseLong(str);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
