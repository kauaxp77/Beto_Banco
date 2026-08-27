package com.betobanco.courses.repository;

import com.betobanco.courses.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    List<Testimonial> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Testimonial> findByStatusOrderByCreatedAtDesc(String status);

    List<Testimonial> findAllByOrderByCreatedAtDesc();
}
