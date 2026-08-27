package com.betobanco.courses.repository;

import com.betobanco.courses.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findByCourseIdOrderByPositionAscTitleAsc(UUID courseId);

    List<CourseModule> findByCourseIdInOrderByPositionAscTitleAsc(Collection<UUID> courseIds);

    long countByCourseId(UUID courseId);
}
