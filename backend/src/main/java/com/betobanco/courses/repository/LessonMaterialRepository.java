package com.betobanco.courses.repository;

import com.betobanco.courses.entity.LessonMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, UUID> {

    List<LessonMaterial> findByLessonIdInOrderByPositionAscTitleAsc(Collection<UUID> lessonIds);
}
