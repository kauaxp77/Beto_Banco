package com.betobanco.courses.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contratos do motor de questoes, agrupados por coesao. */
public final class QuizDtos {

    private QuizDtos() {
    }

    /** Visao do aluno ANTES de entregar: sem gabarito, sem comentario. */
    public record QuizResponse(List<QuestionForStudent> questions,
                               List<AttemptSummary> myAttempts) {
    }

    public record QuestionForStudent(UUID id, String statement, List<String> options,
                                     int position) {
    }

    public record AttemptSummary(UUID id, int correctCount, int totalCount,
                                 Instant createdAt) {
    }

    public record SubmitRequest(@NotEmpty List<@NotNull Answer> answers) {

        public record Answer(@NotNull UUID questionId, @Min(0) @Max(9) int answerIndex) {
        }
    }

    /** Gabarito comentado: so existe DEPOIS da entrega. */
    public record ResultResponse(int correctCount, int totalCount, int scorePct,
                                 List<ResultItem> items) {
    }

    public record ResultItem(UUID questionId, int myIndex, int correctIndex,
                             boolean correct, String explanation) {
    }

    /** Visao do admin: com gabarito e comentario. */
    public record QuestionAdmin(UUID id, String statement, List<String> options,
                                int correctIndex, String explanation, int position) {
    }

    public record QuestionRequest(
            @NotBlank @Size(max = 4000) String statement,
            @NotEmpty @Size(min = 2, max = 5) List<@NotBlank @Size(max = 1000) String> options,
            @Min(0) @Max(4) int correctIndex,
            @Size(max = 4000) String explanation,
            int position) {
    }
}
