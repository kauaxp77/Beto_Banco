package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    /** JSON {questionId: answerIndex} — a prova entregue, imutavel. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String answers;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(UUID userId, UUID lessonId, int correctCount, int totalCount,
                       String answers) {
        this.userId = userId;
        this.lessonId = lessonId;
        this.correctCount = correctCount;
        this.totalCount = totalCount;
        this.answers = answers;
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

    public int getCorrectCount() {
        return correctCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public String getAnswers() {
        return answers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
