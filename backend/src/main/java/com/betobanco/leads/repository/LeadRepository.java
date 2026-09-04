package com.betobanco.leads.repository;

import com.betobanco.leads.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByTenantIdAndEmail(UUID tenantId, String email);

    /**
     * Fila do CRM.
     *
     * <p>Ordena por {@code lastSeenAt} decrescente porque a intencao de compra
     * envelhece: quem teve o cartao recusado hoje de manha atende; quem baixou
     * um PDF ha tres semanas ja esqueceu do assunto.
     *
     * <p>O CAST em cada parametro de texto e obrigatorio. Sem tipo declarado o
     * driver manda o nulo como parametro sem tipo, e o Postgres resolve
     * {@code lower(?)} para {@code lower(bytea)} — funcao que nao existe. A
     * consulta quebraria justamente no caso comum, o de listar sem filtro.
     */
    @Query("""
           SELECT l FROM Lead l
            WHERE l.tenantId = :tenantId
              AND (CAST(:status AS String) IS NULL OR l.status = CAST(:status AS String))
              AND (CAST(:busca AS String) IS NULL
                   OR lower(l.email) LIKE lower(concat('%', CAST(:busca AS String), '%'))
                   OR lower(l.name) LIKE lower(concat('%', CAST(:busca AS String), '%')))
            ORDER BY l.lastSeenAt DESC
           """)
    Page<Lead> buscar(@Param("tenantId") UUID tenantId,
                      @Param("status") String status,
                      @Param("busca") String busca,
                      Pageable paginacao);

    long countByTenantIdAndStatus(UUID tenantId, String status);
}
