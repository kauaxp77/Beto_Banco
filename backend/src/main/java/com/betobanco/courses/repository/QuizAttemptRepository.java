package com.betobanco.courses.repository;

import com.betobanco.courses.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findTop10ByUserIdAndLessonIdOrderByCreatedAtDesc(UUID userId,
                                                                       UUID lessonId);
}
