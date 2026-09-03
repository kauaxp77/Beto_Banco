package br.com.aprovacao.catalogo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CursoRepository extends JpaRepository<Curso, UUID> {

    Optional<Curso> findByTenantIdAndSlug(UUID tenantId, String slug);

    @Query("""
           SELECT c FROM Curso c
            WHERE c.tenantId = :tenantId
              AND c.publicadoEm IS NOT NULL
              AND c.publicadoEm <= CURRENT_TIMESTAMP
              AND (:carreiraId IS NULL OR c.carreiraId = :carreiraId)
              AND (:cursorPublicado IS NULL OR c.publicadoEm < :cursorPublicado)
            ORDER BY c.publicadoEm DESC
           """)
    List<Curso> listarPublicados(@Param("tenantId") UUID tenantId,
                                 @Param("carreiraId") UUID carreiraId,
                                 @Param("cursorPublicado") java.time.Instant cursorPublicado,
                                 Limit limite);
}
