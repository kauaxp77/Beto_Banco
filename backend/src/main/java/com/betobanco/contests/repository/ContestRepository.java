package com.betobanco.contests.repository;

import com.betobanco.contests.entity.Contest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContestRepository extends JpaRepository<Contest, UUID> {

    Optional<Contest> findByTenantIdAndSlug(UUID tenantId, String slug);

    /**
     * Listagem publica, com os filtros que a secao 07 exige.
     *
     * <p>Cada filtro e opcional pelo padrao "parametro nulo desliga a clausula".
     * A alternativa seria montar a consulta por concatenacao, que e como nasce
     * injecao de SQL em codigo que ninguem le com atencao seis meses depois.
     *
     * <p>O DISTINCT existe por causa da juncao com carreiras: um concurso em
     * duas carreiras apareceria duas vezes sem ele — e a secao 07 garante que
     * isso acontece, nao e hipotese.
     *
     * <p>O CAST em :board nao e decoracao. Sem tipo declarado, o driver manda o
     * nulo como parametro sem tipo e o Postgres resolve upper(?) para
     * upper(bytea), funcao que nao existe: a listagem inteira quebra com 500
     * justamente no caso comum, o de nao filtrar por banca. Os outros filtros
     * escapam porque comparam direto com uma coluna, de onde o tipo se infere.
     */
    @Query("""
           SELECT DISTINCT c FROM Contest c
            WHERE c.tenantId = :tenantId
              AND c.publishedAt IS NOT NULL
              AND (:agencyId IS NULL OR c.agencyId = :agencyId)
              AND (CAST(:board AS String) IS NULL
                   OR upper(c.board) = upper(CAST(:board AS String)))
              AND (:status IS NULL OR c.status = :status)
              AND (:educationLevel IS NULL OR c.educationLevel = :educationLevel)
              AND (:salaryMin IS NULL OR c.salaryCents >= :salaryMin)
              AND (:salaryMax IS NULL OR c.salaryCents <= :salaryMax)
              AND (:careerId IS NULL OR EXISTS (
                    SELECT 1 FROM ContestCareer cc
                     WHERE cc.contestId = c.id AND cc.careerId = :careerId))
           """)
    Page<Contest> publicados(@Param("tenantId") UUID tenantId,
                             @Param("careerId") UUID careerId,
                             @Param("agencyId") UUID agencyId,
                             @Param("board") String board,
                             @Param("status") String status,
                             @Param("educationLevel") String educationLevel,
                             @Param("salaryMin") Long salaryMin,
                             @Param("salaryMax") Long salaryMax,
                             Pageable pageable);

    /**
     * Secao 11 — fila de revisao no admin: ficha sem verificacao ha mais de 60
     * dias. NULLS FIRST porque nunca verificada e o caso mais urgente, nao o
     * menos: ela nunca foi conferida contra a fonte oficial.
     */
    @Query("""
           SELECT c FROM Contest c
            WHERE c.tenantId = :tenantId
              AND (c.verifiedAt IS NULL
                   OR c.verifiedAt < :limite)
            ORDER BY c.verifiedAt ASC NULLS FIRST
           """)
    List<Contest> paraRevisar(@Param("tenantId") UUID tenantId,
                              @Param("limite") java.time.Instant limite,
                              Pageable pageable);

    long countByTenantIdAndPublishedAtIsNotNull(UUID tenantId);
}
