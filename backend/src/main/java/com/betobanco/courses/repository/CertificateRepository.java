package com.betobanco.courses.repository;

import com.betobanco.courses.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);

    Optional<Certificate> findByCode(String code);

    List<Certificate> findByUserIdOrderByIssuedAtDesc(UUID userId);
}
