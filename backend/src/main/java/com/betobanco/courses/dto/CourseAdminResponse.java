package com.betobanco.courses.dto;

import java.util.List;
import java.util.UUID;

public record CourseAdminResponse(
        UUID id,
        String title,
        String slug,
        String description,
        String coverUrl,
        boolean published,
        List<UUID> productIds,
        long moduleCount,
        long lessonCount) {
}
