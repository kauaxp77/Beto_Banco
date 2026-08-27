package com.betobanco.entitlements.repository;

import com.betobanco.entitlements.entity.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    Optional<Entitlement> findByUserIdAndProductIdAndRevokedAtIsNull(UUID userId, UUID productId);

    List<Entitlement> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<Entitlement> findBySourceRef(String sourceRef);
}
