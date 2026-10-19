package com.example.commentservice.repository;

import com.example.commentservice.entity.CommentReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommentReplyRepository extends JpaRepository<CommentReply, Long>, JpaSpecificationExecutor<CommentReply> {
    List<CommentReply> findByOrderIdAndDeletedFalse(Long orderId);
    List<CommentReply> findByParentCommentIdAndDeletedFalse(Long parentCommentId);
    List<CommentReply> findByParentReplyIdAndDeletedFalse(Long parentReplyId);
}
