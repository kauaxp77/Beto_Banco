package com.betobanco.privacy.repository;

import com.betobanco.privacy.entity.LegalAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, UUID> {

    List<LegalAcceptance> findByUserIdOrderByAcceptedAtDesc(UUID userId);
}
