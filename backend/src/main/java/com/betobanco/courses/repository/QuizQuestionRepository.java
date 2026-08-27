package com.betobanco.courses.repository;

import com.betobanco.courses.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByLessonIdOrderByPositionAsc(UUID lessonId);

    long countByLessonId(UUID lessonId);

    List<QuizQuestion> findByLessonIdIn(Collection<UUID> lessonIds);
}
