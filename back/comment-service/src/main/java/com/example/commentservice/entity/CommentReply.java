package com.example.commentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment_replies")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentReply extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "readed")
    private Boolean readed = false;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "employee_name")
    private String employeeName;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "reply_images",
            joinColumns = @JoinColumn(name = "reply_id"),
            inverseJoinColumns = @JoinColumn(name = "image_id")
    )
    private List<Image> images = new ArrayList<>();

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "parent_reply_id")
    private Long parentReplyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id", insertable = false, updatable = false)
    private CommentReply parentReply;

    @OneToMany(mappedBy = "parentReply", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentReply> replies = new ArrayList<>();
}
