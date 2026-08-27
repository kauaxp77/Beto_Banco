package com.betobanco.entitlements.repository;

import com.betobanco.entitlements.entity.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    Optional<Entitlement> findByUserIdAndProductIdAndRevokedAtIsNull(UUID userId, UUID productId);

    /**
     * Insere respeitando o indice parcial {@code entitlements_ativo_unico} sem
     * lancar excecao na colisao: no Postgres, uma violacao de constraint aborta
     * a transacao inteira, e {@code conceder()} roda dentro da transacao do
     * processador de webhooks, que precisa continuar utilizavel. Devolve 1 se
     * inseriu, 0 se ja havia concessao vigente.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO entitlements (id, user_id, product_id, source, source_ref)
            VALUES (:id, :userId, :productId, :source, :sourceRef)
            ON CONFLICT (user_id, product_id) WHERE revoked_at IS NULL DO NOTHING
            """, nativeQuery = true)
    int inserirSeNaoHouverVigente(@Param("id") UUID id,
                                  @Param("userId") UUID userId,
                                  @Param("productId") UUID productId,
                                  @Param("source") String source,
                                  @Param("sourceRef") String sourceRef);

    List<Entitlement> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<Entitlement> findBySourceRef(String sourceRef);

}
