package com.example.commentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Comment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "readed")
    private Boolean readed = false;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "employee_name")
    private String employeeName;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentReply> replies = new ArrayList<>();
}
