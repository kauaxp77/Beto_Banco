package com.betobanco.courses.repository;

import com.betobanco.courses.dto.LessonCardResponse;
import com.betobanco.courses.entity.LessonFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LessonFavoriteRepository
        extends JpaRepository<LessonFavorite, LessonFavorite.Chave> {

    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);

    void deleteByUserIdAndLessonId(UUID userId, UUID lessonId);

    /**
     * Favoritos do aluno, do mais recente ao mais antigo.
     *
     * <p>Traz a posicao do playback junto (LEFT JOIN) para o cartao poder abrir
     * de onde parou: favoritar e marcar para voltar, e voltar para o segundo
     * zero de uma aula de 40 minutos nao e voltar.
     */
    @Query("""
           SELECT new com.betobanco.courses.dto.LessonCardResponse(
                    l.id, l.title, c.id, c.title, l.durationSeconds,
                    p.positionSeconds, f.createdAt)
             FROM LessonFavorite f
             JOIN Lesson l ON l.id = f.lessonId
             JOIN CourseModule m ON m.id = l.moduleId
             JOIN Course c ON c.id = m.courseId
             LEFT JOIN LessonPlayback p ON p.lessonId = l.id AND p.userId = f.userId
            WHERE f.userId = :userId
            ORDER BY f.createdAt DESC
           """)
    List<LessonCardResponse> favoritosDe(@Param("userId") UUID userId);
}
