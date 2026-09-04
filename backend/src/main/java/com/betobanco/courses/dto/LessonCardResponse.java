package com.betobanco.courses.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Cartao de aula usado por "continue assistindo", historico e favoritos.
 * Documento Mestre Premium V3.0, secao 5.
 *
 * <p>Traz o curso junto porque as tres listas atravessam cursos: sem o titulo
 * do curso, "Aula 4" aparece tres vezes na tela e nao da para saber de qual
 * materia e cada uma.
 *
 * <p>{@code videoUrl} nao entra: a lista nao verifica matricula, e a checagem
 * de acesso mora em {@code StudentCourseService.detalhar}. Devolver o link aqui
 * seria contornar essa porta.
 */
public record LessonCardResponse(
        UUID lessonId,
        String lessonTitle,
        UUID courseId,
        String courseTitle,
        Integer durationSeconds,
        Integer positionSeconds,
        Instant at) {

    /** Quanto do video ja foi visto, de 0 a 100. Zero quando a duracao e desconhecida. */
    public int percentual() {
        if (durationSeconds == null || durationSeconds <= 0 || positionSeconds == null) {
            return 0;
        }
        return Math.min(100, (int) Math.round(positionSeconds * 100.0 / durationSeconds));
    }
}
