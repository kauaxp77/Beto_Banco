package com.betobanco.courses.repository;

import com.betobanco.courses.entity.LessonRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LessonRatingRepository extends JpaRepository<LessonRating, UUID> {

    Optional<LessonRating> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    long countByLessonIdAndHelpful(UUID lessonId, boolean helpful);

    java.util.List<LessonRating> findByLessonIdIn(java.util.Collection<UUID> lessonIds);
}
