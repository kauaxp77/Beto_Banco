package com.betobanco.courses.repository;

import com.betobanco.courses.entity.LessonComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonCommentRepository extends JpaRepository<LessonComment, UUID> {

    List<LessonComment> findByLessonIdAndStatusOrderByCreatedAtAsc(UUID lessonId, String status);

    Page<LessonComment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<LessonComment> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<LessonComment> findByLessonIdIn(java.util.Collection<UUID> lessonIds);
}
