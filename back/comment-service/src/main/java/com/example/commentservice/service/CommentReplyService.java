package com.example.commentservice.service;

import com.example.commentservice.dto.request.CommentReplyRequest;
import com.example.commentservice.dto.response.CommentReplyResponse;
import com.example.commentservice.entity.CommentReply;
import com.example.commentservice.exception.ResourceNotFoundException;
import com.example.commentservice.mapper.CommentReplyMapper;
import com.example.commentservice.repository.CommentReplyRepository;
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

    public CommentReplyResponse createReply(Long orderId, CommentReplyRequest request) {
        CommentReply reply = commentReplyMapper.toEntity(request);
        reply.setOrderId(orderId);
        reply.setEmployeeId(getCurrentEmployeeId());
        reply.setReaded(false);
        CommentReply saved = commentReplyRepository.save(reply);
        return commentReplyMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentReplyResponse> getRepliesByParentId(Long parentCommentId) {
        return commentReplyRepository.findByParentCommentIdAndDeletedFalse(parentCommentId)
                .stream()
                .map(commentReplyMapper::toDto)
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
        CommentReply saved = commentReplyRepository.save(reply);
        return commentReplyMapper.toDto(saved);
    }

    public CommentReplyResponse markAsRead(Long id) {
        CommentReply reply = commentReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommentReply not found"));
        reply.setReaded(true);
        CommentReply saved = commentReplyRepository.save(reply);
        return commentReplyMapper.toDto(saved);
    }

    public void deleteReply(Long id) {
        CommentReply reply = commentReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommentReply not found"));
        reply.setDeleted(true);
        commentReplyRepository.save(reply);
    }
}
