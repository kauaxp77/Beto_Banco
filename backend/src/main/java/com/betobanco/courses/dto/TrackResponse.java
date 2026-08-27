package com.betobanco.courses.dto;

import java.util.List;
import java.util.UUID;

/**
 * Trilha derivada de um combo: produto que libera 2+ cursos. O progresso e
 * agregado sobre todas as aulas de todos os cursos do combo.
 */
public record TrackResponse(
        UUID productId,
        String title,
        long totalLessons,
        long completedLessons,
        List<TrackCourse> courses) {

    public record TrackCourse(UUID id, String title, long totalLessons, long completedLessons) {
    }
}
