package com.betobanco.courses.repository;

import com.betobanco.courses.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    List<LessonProgress> findByUserIdAndLessonIdIn(UUID userId, Collection<UUID> lessonIds);

    List<LessonProgress> findByLessonIdIn(Collection<UUID> lessonIds);

    List<LessonProgress> findByUserId(UUID userId);
}
