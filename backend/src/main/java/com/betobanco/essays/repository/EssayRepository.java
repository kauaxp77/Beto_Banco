package com.betobanco.essays.repository;

import com.betobanco.essays.entity.Essay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EssayRepository extends JpaRepository<Essay, UUID> {

    List<Essay> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    Optional<Essay> findByIdAndUserId(UUID id, UUID userId);

    /**
     * A fila do corretor, ordenada por vencimento. Secao 14: o prazo de 7 dias
     * so e cumprivel se a fila for drenada por urgencia, e nao por ordem de
     * chegada -- uma redacao que vence amanha tem de vir antes de uma que
     * chegou primeiro mas vence daqui a seis dias.
     */
    @Query("""
           SELECT e FROM Essay e
            WHERE e.status IN ('SUBMITTED', 'IN_REVIEW')
            ORDER BY e.dueAt ASC
           """)
    List<Essay> fila(Pageable limite);

    long countByStatusIn(List<String> status);
}
