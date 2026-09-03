package com.betobanco.contests.repository;

import com.betobanco.contests.entity.Career;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareerRepository extends JpaRepository<Career, UUID> {

    @Query("SELECT c FROM Career c WHERE c.tenantId = :tenantId AND c.active = true "
            + "ORDER BY c.position, c.name")
    List<Career> ativas(@Param("tenantId") UUID tenantId);

    Optional<Career> findByTenantIdAndSlug(UUID tenantId, String slug);
}
