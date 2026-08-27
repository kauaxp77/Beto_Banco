package com.betobanco.courses.dto;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String title,
        String body,
        Instant createdAt) {
}
