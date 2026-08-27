package com.betobanco.courses.repository;

import com.betobanco.courses.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdInOrderByPositionAscTitleAsc(Collection<UUID> moduleIds);

    long countByModuleIdIn(Collection<UUID> moduleIds);
}
