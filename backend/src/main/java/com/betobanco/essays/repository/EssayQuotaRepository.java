package com.betobanco.essays.repository;

import com.betobanco.essays.entity.EssayQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface EssayQuotaRepository extends JpaRepository<EssayQuota, EssayQuota.Chave> {

    Optional<EssayQuota> findByUserIdAndPeriod(UUID userId, LocalDate period);
}
