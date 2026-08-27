package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_comments")
public class LessonComment {

    public static final String VISIBLE = "VISIBLE";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private String status = VISIBLE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected LessonComment() {
    }

    public LessonComment(UUID lessonId, UUID userId, UUID parentId, String body) {
        this.lessonId = lessonId;
        this.userId = userId;
        this.parentId = parentId;
        this.body = body;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
