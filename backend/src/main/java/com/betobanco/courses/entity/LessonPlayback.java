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
 * Onde o aluno parou. Documento Mestre Premium V3.0, secao 5: "Continue
 * assistindo" e "Historico".
 *
 * <p>Tabela separada de {@code lesson_progress} de proposito. Aquela guarda
 * conclusao e so ganha linha quando a aula termina; a posicao existe desde o
 * primeiro segundo. Reaproveita-la exigiria tornar {@code completed_at}
 * anulavel, e toda contagem de "aulas concluidas" que hoje conta linhas
 * passaria a contar tambem aula comecada — o progresso subiria sozinho sem o
 * aluno concluir nada.
 */
@Entity
@Table(name = "lesson_playback")
@IdClass(LessonPlayback.Chave.class)
public class LessonPlayback {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "position_seconds", nullable = false)
    private int positionSeconds;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected LessonPlayback() {
    }

    public LessonPlayback(UUID userId, UUID lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }

    /**
     * Move a marca de onde parou.
     *
     * <p>Recusa segundo negativo em vez de corrigir em silencio: um player que
     * manda posicao negativa esta com defeito, e gravar zero esconderia isso
     * ate alguem reclamar que o "continuar" volta sempre para o comeco.
     */
    public void marcar(int segundos) {
        if (segundos < 0) {
            throw new IllegalArgumentException("Posição do vídeo não pode ser negativa");
        }
        this.positionSeconds = segundos;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public int getPositionSeconds() {
        return positionSeconds;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
