package com.betobanco.courses.dto;

import java.time.Instant;
import java.util.UUID;

/** Fila de moderacao: o admin ve tudo, inclusive ocultos. */
public record CommentAdminResponse(
        UUID id,
        UUID lessonId,
        String lessonTitle,
        UUID parentId,
        String body,
        String authorName,
        String authorEmail,
        String status,
        Instant createdAt) {
}
