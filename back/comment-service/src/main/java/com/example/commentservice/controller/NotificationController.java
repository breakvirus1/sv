package com.example.commentservice.controller;

import com.example.commentservice.dto.response.NotificationResponse;
import com.example.commentservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Service", description = "API для управления уведомлениями")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Получить все уведомления текущего пользователя")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotificationsForCurrentUser());
    }

    @Operation(summary = "Получить количество непрочитанных уведомлений")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCountForCurrentUser());
    }

    @Operation(summary = "Отметить уведомление как прочитанное")
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markAsRead(@Parameter(description = "ID уведомления") @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @Operation(summary = "Отметить все уведомления как прочитанные")
    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить уведомление")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteNotification(@Parameter(description = "ID уведомления") @PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
