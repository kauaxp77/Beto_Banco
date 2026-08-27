package com.betobanco.courses.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Tudo que a area de discussao de uma aula precisa, em uma chamada. */
public record LessonDiscussionResponse(
        List<CommentResponse> comments,
        long helpfulCount,
        long notHelpfulCount,
        Boolean myRating) {

    public record CommentResponse(
            UUID id,
            UUID parentId,
            String body,
            String authorName,
            boolean instructor,
            boolean mine,
            Instant createdAt) {
    }
}
