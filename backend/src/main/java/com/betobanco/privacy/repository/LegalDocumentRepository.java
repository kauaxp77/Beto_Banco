package com.betobanco.privacy.repository;

import com.betobanco.privacy.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    /**
     * A versao vigente e a mais recente que ja entrou em vigor. O filtro por
     * data permite publicar um texto com vigencia futura sem que ele passe a
     * valer antes da hora.
     */
    @Query("""
           SELECT d FROM LegalDocument d
            WHERE d.tenantId = :tenantId
              AND d.type = :type
              AND d.effectiveFrom <= CURRENT_TIMESTAMP
            ORDER BY d.effectiveFrom DESC
            LIMIT 1
           """)
    Optional<LegalDocument> vigente(@Param("tenantId") UUID tenantId, @Param("type") String type);

    Optional<LegalDocument> findByTenantIdAndTypeAndVersion(UUID tenantId, String type, String version);
}
