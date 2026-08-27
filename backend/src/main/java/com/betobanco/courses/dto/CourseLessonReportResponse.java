package com.betobanco.courses.dto;

import java.util.List;
import java.util.UUID;

/** Detalhe por aula: onde a turma avanca e onde abandona. */
public record CourseLessonReportResponse(
        UUID courseId,
        String courseTitle,
        long students,
        List<LessonLine> lessons) {

    public record LessonLine(
            UUID id,
            String title,
            String moduleTitle,
            long completions,
            int completionPct,
            long helpful,
            long notHelpful,
            long comments) {
    }
}
