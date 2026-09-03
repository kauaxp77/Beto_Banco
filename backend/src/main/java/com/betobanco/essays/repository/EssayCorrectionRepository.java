package com.betobanco.essays.repository;

import com.betobanco.essays.entity.EssayCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EssayCorrectionRepository extends JpaRepository<EssayCorrection, UUID> {

    Optional<EssayCorrection> findByEssayId(UUID essayId);
}
