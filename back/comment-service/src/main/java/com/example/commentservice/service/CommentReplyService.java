package com.example.commentservice.service;

import com.example.commentservice.dto.request.CommentReplyRequest;
import com.example.commentservice.dto.response.CommentReplyResponse;
import com.example.commentservice.entity.Comment;
import com.example.commentservice.entity.CommentReply;
import com.example.commentservice.exception.ResourceNotFoundException;
import com.example.commentservice.mapper.CommentReplyMapper;
import com.example.commentservice.repository.CommentReplyRepository;
import com.example.commentservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class CommentReplyService {

    private final CommentReplyRepository commentReplyRepository;
    private final CommentReplyMapper commentReplyMapper;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;

    private String getCurrentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("Unsupported authentication type");
    }

    private Long getCurrentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("Username not found in token");
            }
            String token = jwtAuth.getToken().getTokenValue();
            var headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            var requestEntity = new org.springframework.http.HttpEntity<>(headers);
            try {
                var responseType = new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
                var response = restTemplate.exchange(
                        "http://employee-service:8083/api/v1/employees/username/{username}",
                        org.springframework.http.HttpMethod.GET,
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

    private String getCurrentEmployeeNameFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String fullName = jwtAuth.getToken().getClaimAsString("name");
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (fullName != null && !fullName.isBlank() && username != null && !username.isBlank()) {
                return fullName + " (" + username + ")";
            }
            if (fullName != null && !fullName.isBlank()) {
                return fullName;
            }
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        return "Неизвестный сотрудник";
    }

    private String fetchEmployeeName(Long employeeId) {
        if (employeeId == null) return "Неизвестный сотрудник";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = auth instanceof JwtAuthenticationToken jwtAuth ? jwtAuth.getToken().getTokenValue() : null;
        var headers = new org.springframework.http.HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        var requestEntity = new org.springframework.http.HttpEntity<>(headers);
        try {
            var responseType = new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
            var response = restTemplate.exchange(
                    "http://employee-service:8083/api/v1/employees/{id}",
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    responseType,
                    employeeId
            );
            var body = response.getBody();
            if (body == null) return "Неизвестный сотрудник";
            String firstName = body.get("firstName") != null ? body.get("firstName").toString() : "";
            String lastName = body.get("lastName") != null ? body.get("lastName").toString() : "";
            String fullName = (lastName + " " + firstName).trim();
            return fullName.isEmpty() ? "Сотрудник #" + employeeId : fullName;
        } catch (Exception e) {
            return "Сотрудник #" + employeeId;
        }
    }

    public CommentReplyResponse createReply(Long orderId, CommentReplyRequest request) {
        CommentReply reply = commentReplyMapper.toEntity(request);

        if (request.getParentReplyId() != null) {
            CommentReply parentReply = commentReplyRepository.findById(request.getParentReplyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
            reply.setOrderId(parentReply.getOrderId());
            reply.setParentReplyId(request.getParentReplyId());
            reply.setParentCommentId(parentReply.getParentCommentId());
        } else {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            reply.setOrderId(parentComment.getOrderId());
            reply.setParentCommentId(request.getParentCommentId());
        }

        reply.setEmployeeId(getCurrentEmployeeId());
        reply.setEmployeeName(getCurrentEmployeeNameFromToken());
        reply.setReaded(false);
        CommentReply saved = commentReplyRepository.save(reply);

        try {
            Long targetUserId = null;
            String message = null;

            if (request.getParentReplyId() != null) {
                CommentReply parentReply = commentReplyRepository.findById(request.getParentReplyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent reply not found"));
                targetUserId = parentReply.getEmployeeId();
                message = "Новый ответ на ваш комментарий: " + saved.getBody();
            } else {
                Comment parentComment = commentRepository.findById(request.getParentCommentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
                targetUserId = parentComment.getEmployeeId();
                message = "Новый ответ на ваш комментарий: " + saved.getBody();
            }

            if (targetUserId != null && !targetUserId.equals(saved.getEmployeeId())) {
                boolean notified = false;
                try {
                    var responseType = new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
                    var headers = new org.springframework.http.HttpHeaders();
                    headers.setBearerAuth(getCurrentToken());
                    var requestEntity = new org.springframework.http.HttpEntity<>(headers);
                    var orderResponse = restTemplate.exchange(
                            "http://order-service:8081/api/v1/orders/" + saved.getOrderId(),
                            org.springframework.http.HttpMethod.GET,
                            requestEntity,
                            responseType
                    );
                    var orderBody = orderResponse.getBody();
                    if (orderBody != null && orderBody.containsKey("status")) {
                        Object statusObj = orderBody.get("status");
                        String status = statusObj != null ? statusObj.toString() : null;
                        if (!"READY".equals(status) && !"CLOSED".equals(status)) {
                            notificationService.createNotification(message, "REPLY", saved.getOrderId(), "REPLY", saved.getId(), targetUserId);
                            notified = true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to check order status for reply notification: " + e.getMessage());
                }
                if (!notified) {
                    notificationService.createNotification(message, "REPLY", saved.getOrderId(), "REPLY", saved.getId(), targetUserId);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send reply notification: " + e.getMessage());
        }

        return buildReplyDto(saved);
    }

    private CommentReplyResponse buildReplyDto(CommentReply reply) {
        var dto = commentReplyMapper.toDto(reply);
        dto.setEmployeeName(
                reply.getEmployeeName() != null && !reply.getEmployeeName().isBlank()
                        ? reply.getEmployeeName()
                        : fetchEmployeeName(reply.getEmployeeId())
        );
        if (reply.getReplies() != null && !reply.getReplies().isEmpty()) {
            dto.setReplies(
                    reply.getReplies().stream()
                            .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                            .map(this::buildReplyDto)
                            .toList()
            );
        } else {
            dto.setReplies(List.of());
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<CommentReplyResponse> getRepliesByParentId(Long parentCommentId) {
        return commentReplyRepository.findByParentCommentIdAndDeletedFalse(parentCommentId)
                .stream()
                .filter(reply -> reply.getParentReplyId() == null)
                .map(this::buildReplyDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CommentReplyResponse> getAllReplies(Pageable pageable) {
        return commentReplyRepository.findAll(pageable)
                .map(commentReplyMapper::toDto);
    }

    public CommentReplyResponse updateReply(Long id, CommentReplyRequest request) {
        CommentReply reply = commentReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommentReply not found"));
        reply.setBody(request.getBody());
        reply.setParentCommentId(request.getParentCommentId());
        if (request.getParentReplyId() != null) {
            reply.setParentReplyId(request.getParentReplyId());
        }
        CommentReply saved = commentReplyRepository.save(reply);
        return buildReplyDto(saved);
    }

    public CommentReplyResponse markAsRead(Long id) {
        CommentReply reply = commentReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommentReply not found"));
        reply.setReaded(true);
        CommentReply saved = commentReplyRepository.save(reply);
        return buildReplyDto(saved);
    }

    public void deleteReply(Long id) {
        CommentReply reply = commentReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommentReply not found"));
        reply.setDeleted(true);
        commentReplyRepository.save(reply);
    }
}
