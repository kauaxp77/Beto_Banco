package com.betobanco.courses.repository;

import com.betobanco.courses.entity.CourseProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseProductRepository extends JpaRepository<CourseProduct, UUID> {

    List<CourseProduct> findByProductIdIn(Collection<UUID> productIds);

    List<CourseProduct> findByCourseId(UUID courseId);

    Optional<CourseProduct> findByCourseIdAndProductId(UUID courseId, UUID productId);
}
