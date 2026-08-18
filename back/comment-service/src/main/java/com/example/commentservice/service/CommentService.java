package com.example.commentservice.service;

import com.example.commentservice.dto.request.CommentRequest;
import com.example.commentservice.dto.response.CommentResponse;
import com.example.commentservice.entity.Comment;
import com.example.commentservice.entity.CommentReply;
import com.example.commentservice.entity.Image;
import com.example.commentservice.exception.ResourceNotFoundException;
import com.example.commentservice.mapper.CommentMapper;
import com.example.commentservice.mapper.CommentReplyMapper;
import com.example.commentservice.mapper.ImageMapper;
import com.example.commentservice.repository.CommentRepository;
import com.example.commentservice.repository.CommentReplyRepository;
import com.example.commentservice.repository.ImageRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final RestTemplate restTemplate;
    private final CommentReplyMapper commentReplyMapper;
    private final CommentReplyRepository commentReplyRepository;
    private final NotificationService notificationService;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final ImageService imageService;

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

    public CommentResponse createComment(Long orderId, CommentRequest request) {
        Comment comment = commentMapper.toEntity(request);
        comment.setOrderId(orderId);
        comment.setEmployeeId(getCurrentEmployeeId());
        comment.setEmployeeName(getCurrentEmployeeNameFromToken());
        comment.setReaded(false);

        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            List<Image> images = imageRepository.findAllById(request.getImageIds());
            comment.setImages(images);
        }

        Comment saved = commentRepository.save(comment);

        try {
            var responseType = new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
            var headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(getCurrentToken());
            var requestEntity = new org.springframework.http.HttpEntity<>(headers);
            var orderResponse = restTemplate.exchange(
                    "http://order-service:8081/api/v1/orders/" + orderId,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    responseType
            );
            var orderBody = orderResponse.getBody();
            if (orderBody != null && orderBody.containsKey("manager") && orderBody.containsKey("status")) {
                Object statusObj = orderBody.get("status");
                String status = statusObj != null ? statusObj.toString() : null;
                if ("READY".equals(status) || "CLOSED".equals(status)) {
                    return commentMapper.toDto(saved);
                }
                Object managerObj = orderBody.get("manager");
                if (managerObj instanceof java.util.Map) {
                    Object authorIdObj = ((java.util.Map<?, ?>) managerObj).get("id");
                    Long authorId = authorIdObj instanceof Number n ? n.longValue() : Long.parseLong(authorIdObj.toString());
                    if (!authorId.equals(comment.getEmployeeId())) {
                        String message = "Новый комментарий к заказу №" + orderBody.get("orderNumber") + ": " + comment.getBody();
                        notificationService.createNotification(message, "COMMENT", orderId, "COMMENT", saved.getId(), authorId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send comment notification: " + e.getMessage());
        }

        return commentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByOrderId(Long orderId) {
        return commentRepository.findByOrderIdAndDeletedFalse(orderId)
                .stream()
                .map(comment -> {
                    var dto = commentMapper.toDto(comment);
                    dto.setEmployeeName(
                            comment.getEmployeeName() != null && !comment.getEmployeeName().isBlank()
                                    ? comment.getEmployeeName()
                                    : fetchEmployeeName(comment.getEmployeeId())
                    );
                    if (comment.getImages() != null && !comment.getImages().isEmpty()) {
                        dto.setImages(comment.getImages().stream()
                                .map(imageMapper::toDto)
                                .collect(Collectors.toList()));
                    } else {
                        dto.setImages(List.of());
                    }
                    if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                        var rootReplies = comment.getReplies().stream()
                                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()) && r.getParentReplyId() == null)
                                .map(reply -> {
                                    var replyDto = commentReplyMapper.toDto(reply);
                                    replyDto.setEmployeeName(
                                            reply.getEmployeeName() != null && !reply.getEmployeeName().isBlank()
                                                    ? reply.getEmployeeName()
                                                    : fetchEmployeeName(reply.getEmployeeId())
                                    );
                                    if (reply.getImages() != null && !reply.getImages().isEmpty()) {
                                        replyDto.setImages(reply.getImages().stream()
                                                .map(imageMapper::toDto)
                                                .collect(Collectors.toList()));
                                    } else {
                                        replyDto.setImages(List.of());
                                    }
                                    replyDto.setReplies(buildNestedReplies(reply));
                                    return replyDto;
                                })
                                .toList();
                        dto.setReplies(rootReplies);
                    } else {
                        dto.setReplies(List.of());
                    }
                    return dto;
                })
                .toList();
    }

    private List<com.example.commentservice.dto.response.CommentReplyResponse> buildNestedReplies(CommentReply reply) {
        if (reply.getReplies() == null || reply.getReplies().isEmpty()) {
            return List.of();
        }
        return reply.getReplies().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .map(child -> {
                    var dto = commentReplyMapper.toDto(child);
                    dto.setEmployeeName(
                            child.getEmployeeName() != null && !child.getEmployeeName().isBlank()
                                    ? child.getEmployeeName()
                                    : fetchEmployeeName(child.getEmployeeId())
                    );
                    if (child.getImages() != null && !child.getImages().isEmpty()) {
                        dto.setImages(child.getImages().stream()
                                .map(imageMapper::toDto)
                                .collect(Collectors.toList()));
                    } else {
                        dto.setImages(List.of());
                    }
                    dto.setReplies(buildNestedReplies(child));
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getAllComments(Pageable pageable) {
        return commentRepository.findAll(pageable)
                .map(commentMapper::toDto);
    }

    public CommentResponse updateComment(Long id, CommentRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setBody(request.getBody());
        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }

    public CommentResponse markAsRead(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setReaded(true);
        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }

    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setDeleted(true);
        commentRepository.save(comment);
    }
}
