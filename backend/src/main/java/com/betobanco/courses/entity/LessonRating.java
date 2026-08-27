package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "lesson_ratings")
public class LessonRating {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private boolean helpful;

    protected LessonRating() {
    }

    public LessonRating(UUID lessonId, UUID userId, boolean helpful) {
        this.lessonId = lessonId;
        this.userId = userId;
        this.helpful = helpful;
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

    public boolean isHelpful() {
        return helpful;
    }

    public void setHelpful(boolean helpful) {
        this.helpful = helpful;
    }
}
