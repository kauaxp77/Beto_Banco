package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "testimonials")
public class Testimonial {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private String status = PENDING;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Testimonial() {
    }

    public Testimonial(UUID userId, UUID courseId, String body) {
        this.userId = userId;
        this.courseId = courseId;
        this.body = body;
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
