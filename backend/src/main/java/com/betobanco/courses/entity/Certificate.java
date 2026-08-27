package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int hours;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private Instant issuedAt;

    protected Certificate() {
    }

    public Certificate(UUID userId, UUID courseId, String code, int hours) {
        this.userId = userId;
        this.courseId = courseId;
        this.code = code;
        this.hours = hours;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getCode() {
        return code;
    }

    public int getHours() {
        return hours;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
