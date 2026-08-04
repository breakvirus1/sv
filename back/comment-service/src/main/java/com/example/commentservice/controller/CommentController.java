package com.example.commentservice.controller;

import com.example.commentservice.dto.request.CommentRequest;
import com.example.commentservice.dto.response.CommentResponse;
import com.example.commentservice.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Service", description = "API для управления комментариями")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Получить комментарии по orderId")
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentResponse>> getCommentsByOrderId(
            @Parameter(description = "ID заказа") @PathVariable Long orderId) {
        return ResponseEntity.ok(commentService.getCommentsByOrderId(orderId));
    }

    @Operation(summary = "Получить все комментарии с пагинацией")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<CommentResponse>> getAllComments(Pageable pageable) {
        return ResponseEntity.ok(commentService.getAllComments(pageable));
    }

    @Operation(summary = "Создать комментарий")
    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "ID заказа") @PathVariable Long orderId,
            @RequestBody CommentRequest request) {
        CommentResponse response = commentService.createComment(orderId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Отметить комментарий как прочитанный")
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.markAsRead(id));
    }

    @Operation(summary = "Обновить комментарий")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID комментария") @PathVariable Long id,
            @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(id, request));
    }

    @Operation(summary = "Удалить комментарий")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID комментария") @PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
