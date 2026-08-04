package com.example.commentservice.controller;

import com.example.commentservice.dto.request.CommentReplyRequest;
import com.example.commentservice.dto.response.CommentReplyResponse;
import com.example.commentservice.service.CommentReplyService;
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
@RequestMapping("/api/v1/comment-replies")
@RequiredArgsConstructor
@Tag(name = "Comment Reply Service", description = "API для управления ответами на комментарии")
public class CommentReplyController {

    private final CommentReplyService commentReplyService;

    @Operation(summary = "Получить ответы по parentCommentId")
    @GetMapping("/parent/{parentCommentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentReplyResponse>> getRepliesByParentId(
            @Parameter(description = "ID родительского комментария") @PathVariable Long parentCommentId) {
        return ResponseEntity.ok(commentReplyService.getRepliesByParentId(parentCommentId));
    }

    @Operation(summary = "Получить все ответы с пагинацией")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<CommentReplyResponse>> getAllReplies(Pageable pageable) {
        return ResponseEntity.ok(commentReplyService.getAllReplies(pageable));
    }

    @Operation(summary = "Создать ответ")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    public ResponseEntity<CommentReplyResponse> createReply(@RequestBody CommentReplyRequest request) {
        CommentReplyResponse response = commentReplyService.createReply(request.getParentCommentId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Отметить ответ как прочитанный")
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentReplyResponse> markReplyAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(commentReplyService.markAsRead(id));
    }

    @Operation(summary = "Обновить ответ")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CommentReplyResponse> updateReply(
            @Parameter(description = "ID ответа") @PathVariable Long id,
            @RequestBody CommentReplyRequest request) {
        return ResponseEntity.ok(commentReplyService.updateReply(id, request));
    }

    @Operation(summary = "Удалить ответ")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteReply(
            @Parameter(description = "ID ответа") @PathVariable Long id) {
        commentReplyService.deleteReply(id);
        return ResponseEntity.noContent().build();
    }
}
