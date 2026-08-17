package com.example.commentservice.service;

import com.example.commentservice.dto.response.NotificationResponse;
import com.example.commentservice.entity.Notification;
import com.example.commentservice.exception.ResourceNotFoundException;
import com.example.commentservice.mapper.NotificationMapper;
import com.example.commentservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final RestTemplate restTemplate;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("Username not found in token");
            }
            try {
                var responseType = new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
                var tokenValue = jwtAuth.getToken().getTokenValue();
                var headers = new HttpHeaders();
                headers.setBearerAuth(tokenValue);
                headers.setContentType(MediaType.APPLICATION_JSON);
                var requestEntity = new HttpEntity<>(headers);
                var response = restTemplate.exchange(
                        "http://employee-service:8083/api/v1/employees/username/{username}",
                        HttpMethod.GET,
                        requestEntity,
                        responseType,
                        username
                );
                var body = response.getBody();
                if (body == null || !body.containsKey("id")) {
                    throw new IllegalStateException("Employee not found for username: " + username);
                }
                Object idObj = body.get("id");
                if (idObj instanceof Number n) {
                    return n.longValue();
                }
                return Long.parseLong(idObj.toString());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to fetch employee for username: " + username, e);
            }
        }
        throw new IllegalStateException("Unsupported authentication type");
    }

    public NotificationResponse createNotification(String message, String type, Long referenceId, String referenceType, Long referenceSubId, Long userId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setReferenceSubId(referenceSubId);
        notification.setReaded(false);
        notificationRepository.save(notification);
        return notificationMapper.toDto(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForCurrentUser() {
        Long userId = getCurrentUserId();
        return notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForCurrentUser() {
        Long userId = getCurrentUserId();
        return notificationRepository.countByUserIdAndReadedFalseAndDeletedFalse(userId);
    }

    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setReaded(true);
        notificationRepository.save(notification);
        return notificationMapper.toDto(notification);
    }

    public void markAllAsRead() {
        Long userId = getCurrentUserId();
        List<Notification> notifications = notificationRepository.findByUserIdAndReadedFalseAndDeletedFalseOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setReaded(true));
        notificationRepository.saveAll(notifications);
    }

    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setDeleted(true);
        notificationRepository.save(notification);
    }
}
