package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aula marcada para voltar depois. Documento Mestre Premium V3.0, secao 5.
 *
 * <p>Na aula, e nao no curso: o aluno marca o ponto ao qual quer voltar na
 * revisao, e "curso favorito" nao diz onde estava a duvida.
 */
@Entity
@Table(name = "lesson_favorites")
@IdClass(LessonFavorite.Chave.class)
public class LessonFavorite {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected LessonFavorite() {
    }

    public LessonFavorite(UUID userId, UUID lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Chave implements Serializable {
        private UUID userId;
        private UUID lessonId;

        public Chave() {
        }

        public Chave(UUID userId, UUID lessonId) {
            this.userId = userId;
            this.lessonId = lessonId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Chave outra)) {
                return false;
            }
            return Objects.equals(userId, outra.userId)
                    && Objects.equals(lessonId, outra.lessonId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, lessonId);
        }
    }
}
