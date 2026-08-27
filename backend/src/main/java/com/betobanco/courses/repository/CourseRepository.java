package com.betobanco.courses.repository;

import com.betobanco.courses.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findBySlug(String slug);

    List<Course> findByIdInAndPublishedTrueOrderByTitle(Collection<UUID> ids);
}
