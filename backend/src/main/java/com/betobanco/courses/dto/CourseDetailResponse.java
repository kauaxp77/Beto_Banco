package com.betobanco.courses.dto;

import java.util.List;
import java.util.UUID;

public record CourseDetailResponse(
        UUID id,
        String title,
        String description,
        String coverUrl,
        List<ModuleResponse> modules) {

    public record ModuleResponse(UUID id, String title, int position,
                                 List<LessonResponse> lessons) {
    }

    public record LessonResponse(UUID id, String title, String description, String videoUrl,
                                 Integer durationSeconds, int position, boolean completed,
                                 List<MaterialResponse> materials) {
    }

    public record MaterialResponse(UUID id, String title, String url) {
    }
}
