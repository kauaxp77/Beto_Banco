package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "completed_at", insertable = false, updatable = false)
    private Instant completedAt;

    protected LessonProgress() {
    }

    public LessonProgress(UUID userId, UUID lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
