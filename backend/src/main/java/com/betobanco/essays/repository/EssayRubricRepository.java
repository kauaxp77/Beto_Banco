package com.betobanco.essays.repository;

import com.betobanco.essays.entity.EssayRubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EssayRubricRepository extends JpaRepository<EssayRubric, UUID> {

    @Query("""
           SELECT r FROM EssayRubric r
            WHERE r.tenantId = :tenantId AND upper(r.board) = upper(:board) AND r.active = true
           """)
    Optional<EssayRubric> ativaDaBanca(@Param("tenantId") UUID tenantId, @Param("board") String board);

    @Query("SELECT r FROM EssayRubric r WHERE r.tenantId = :tenantId AND r.active = true ORDER BY r.board")
    List<EssayRubric> ativas(@Param("tenantId") UUID tenantId);
}
