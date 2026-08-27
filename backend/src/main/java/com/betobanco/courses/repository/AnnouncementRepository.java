package com.betobanco.courses.repository;

import com.betobanco.courses.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    /** Gerais (courseId nulo) + dos cursos informados, mais recentes primeiro. */
    @Query("select a from Announcement a "
            + "where a.courseId is null or a.courseId in :courseIds "
            + "order by a.createdAt desc")
    List<Announcement> paraCursos(@Param("courseIds") Collection<UUID> courseIds);

    @Query("select a from Announcement a where a.courseId is null order by a.createdAt desc")
    List<Announcement> gerais();
}
