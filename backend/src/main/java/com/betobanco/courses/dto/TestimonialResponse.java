package com.betobanco.courses.dto;

import java.time.Instant;
import java.util.UUID;

public record TestimonialResponse(
        UUID id,
        String authorName,
        String authorEmail,
        UUID courseId,
        String courseTitle,
        String body,
        String status,
        Instant createdAt) {
}
