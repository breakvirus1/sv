package com.example.commentservice.service;

import com.example.commentservice.dto.request.CommentRequest;
import com.example.commentservice.dto.response.CommentResponse;
import com.example.commentservice.entity.Comment;
import com.example.commentservice.exception.ResourceNotFoundException;
import com.example.commentservice.mapper.CommentMapper;
import com.example.commentservice.mapper.CommentReplyMapper;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final RestTemplate restTemplate;
    private final CommentReplyMapper commentReplyMapper;

    private Long getCurrentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("Username not found in token");
            }
            try {
                var responseType = new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {};
                var response = restTemplate.exchange(
                        "http://employee-service/api/v1/employees/username/{username}",
                        org.springframework.http.HttpMethod.GET,
                        null,
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

    public CommentResponse createComment(Long orderId, CommentRequest request) {
        Comment comment = commentMapper.toEntity(request);
        comment.setOrderId(orderId);
        comment.setEmployeeId(getCurrentEmployeeId());
        comment.setReaded(false);
        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByOrderId(Long orderId) {
        return commentRepository.findByOrderIdAndDeletedFalse(orderId)
                .stream()
                .map(comment -> {
                    var dto = commentMapper.toDto(comment);
                    dto.setReplies(
                            comment.getReplies().stream()
                                    .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                                    .map(commentReplyMapper::toDto)
                                    .toList()
                    );
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
