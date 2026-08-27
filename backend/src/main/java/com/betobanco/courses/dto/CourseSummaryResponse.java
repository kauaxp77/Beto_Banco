package com.betobanco.courses.dto;

import java.util.UUID;

/** Cartao de curso na home do aluno: identidade + progresso agregado. */
public record CourseSummaryResponse(
        UUID id,
        String title,
        String slug,
        String description,
        String coverUrl,
        long totalLessons,
        long completedLessons,
        UUID nextLessonId) {
}
