package com.example.commentservice.controller;

import com.example.commentservice.dto.response.ImageResponse;
import com.example.commentservice.entity.Image;
import com.example.commentservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Image Service", description = "API для загрузки и получения изображений")
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "Загрузить изображения")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ImageResponse>> uploadImages(
            @Parameter(description = "Файлы изображений") @RequestParam("files") List<MultipartFile> files) {
        List<Image> images = imageService.uploadImages(files);
        List<ImageResponse> responses = images.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @Operation(summary = "Получить изображение по ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> getImage(
            @Parameter(description = "ID изображения") @PathVariable Long id,
            HttpServletResponse response) throws IOException {
        Image image = imageService.getImage(id);
        FileSystemResource resource = new FileSystemResource(image.getPath());
        String contentType = image.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            response.setContentType(contentType);
        } else {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getOriginalName() + "\"");
        return ResponseEntity.ok()
                .contentLength(image.getSize())
                .body(resource);
    }

    @Operation(summary = "Удалить изображение")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteImage(@Parameter(description = "ID изображения") @PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }

    private ImageResponse toResponse(Image image) {
        return new ImageResponse(
                image.getId(),
                image.getFilename(),
                image.getOriginalName(),
                image.getContentType(),
                image.getSize(),
                "/api/v1/images/" + image.getId(),
                image.getCreatedAt()
        );
    }
}
