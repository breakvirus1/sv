package com.example.commentservice.repository;

import com.example.commentservice.entity.CommentReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {
    List<CommentReply> findByOrderIdAndDeletedFalse(Long orderId);
    List<CommentReply> findByParentCommentIdAndDeletedFalse(Long parentCommentId);
    List<CommentReply> findByParentReplyIdAndDeletedFalse(Long parentReplyId);
}
