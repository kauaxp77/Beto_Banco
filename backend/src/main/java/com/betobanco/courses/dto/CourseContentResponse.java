package com.betobanco.courses.dto;

import java.util.List;
import java.util.UUID;

/** Estrutura completa para o editor do admin — inclui aulas nao publicadas. */
public record CourseContentResponse(
        UUID id,
        String title,
        List<ModuleContent> modules) {

    public record ModuleContent(UUID id, String title, int position,
                                List<LessonContent> lessons) {
    }

    public record LessonContent(UUID id, String title, String description, String videoUrl,
                                Integer durationSeconds, int position, boolean published,
                                List<MaterialContent> materials, long questionCount) {
    }

    public record MaterialContent(UUID id, String title, String url) {
    }
}
