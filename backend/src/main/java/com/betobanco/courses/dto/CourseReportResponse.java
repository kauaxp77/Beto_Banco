package com.betobanco.courses.dto;

import java.util.UUID;

/** Linha do relatorio geral: como cada curso engaja a turma. */
public record CourseReportResponse(
        UUID id,
        String title,
        boolean published,
        long students,
        long started,
        long totalLessons,
        int avgCompletionPct) {
}
