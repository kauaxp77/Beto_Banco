package com.betobanco.courses.repository;

import com.betobanco.courses.dto.LessonCardResponse;
import com.betobanco.courses.entity.LessonPlayback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonPlaybackRepository
        extends JpaRepository<LessonPlayback, LessonPlayback.Chave> {

    Optional<LessonPlayback> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    /**
     * "Continue assistindo": aulas comecadas e ainda nao concluidas.
     *
     * <p>O NOT EXISTS sobre {@code LessonProgress} e o que faz a lista servir
     * para alguma coisa. Sem ele, a aula que o aluno acabou de terminar ficaria
     * em primeiro lugar na fila de "continuar" — que e exatamente onde ela nao
     * deve estar.
     *
     * <p>As junções são explícitas com ON porque as entidades guardam o id
     * cru ({@code moduleId}, {@code courseId}) em vez de associações.
     */
    @Query("""
           SELECT new com.betobanco.courses.dto.LessonCardResponse(
                    l.id, l.title, c.id, c.title, l.durationSeconds,
                    p.positionSeconds, p.updatedAt)
             FROM LessonPlayback p
             JOIN Lesson l ON l.id = p.lessonId
             JOIN CourseModule m ON m.id = l.moduleId
             JOIN Course c ON c.id = m.courseId
            WHERE p.userId = :userId
              AND l.published = true
              AND NOT EXISTS (SELECT 1 FROM LessonProgress g
                               WHERE g.userId = :userId AND g.lessonId = l.id)
            ORDER BY p.updatedAt DESC
           """)
    List<LessonCardResponse> continuarAssistindo(@Param("userId") UUID userId,
                                                 Pageable limite);

    /** Historico: tudo que foi aberto, concluido ou nao, do mais recente ao mais antigo. */
    @Query("""
           SELECT new com.betobanco.courses.dto.LessonCardResponse(
                    l.id, l.title, c.id, c.title, l.durationSeconds,
                    p.positionSeconds, p.updatedAt)
             FROM LessonPlayback p
             JOIN Lesson l ON l.id = p.lessonId
             JOIN CourseModule m ON m.id = l.moduleId
             JOIN Course c ON c.id = m.courseId
            WHERE p.userId = :userId
            ORDER BY p.updatedAt DESC
           """)
    List<LessonCardResponse> historico(@Param("userId") UUID userId, Pageable limite);
}
