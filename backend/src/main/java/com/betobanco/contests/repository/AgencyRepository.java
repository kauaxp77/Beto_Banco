package com.betobanco.contests.repository;

import com.betobanco.contests.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    @Query("SELECT a FROM Agency a WHERE a.tenantId = :tenantId ORDER BY a.name")
    List<Agency> todas(@Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM Agency a WHERE a.tenantId = :tenantId AND upper(a.acronym) = upper(:acronym)")
    Optional<Agency> porSigla(@Param("tenantId") UUID tenantId, @Param("acronym") String acronym);
}
