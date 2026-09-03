package com.betobanco.privacy.repository;

import com.betobanco.privacy.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    /**
     * Vale sempre a decisao mais recente da finalidade. A tabela e append-only,
     * entao "o consentimento atual" e uma consulta, nao um campo.
     */
    @Query("""
           SELECT c FROM Consent c
            WHERE c.userId = :userId AND c.purpose = :purpose
            ORDER BY c.recordedAt DESC
            LIMIT 1
           """)
    Optional<Consent> vigente(@Param("userId") UUID userId, @Param("purpose") String purpose);

    /** Historico completo — o titular tem direito de ver o proprio rastro. */
    List<Consent> findByUserIdOrderByRecordedAtDesc(UUID userId);
}
