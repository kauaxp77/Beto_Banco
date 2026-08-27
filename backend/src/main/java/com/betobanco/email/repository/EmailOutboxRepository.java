package com.betobanco.email.repository;

import com.betobanco.email.entity.EmailOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    Optional<EmailOutbox> findByDedupKey(String dedupKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout",
            value = "-2"))
    @Query("SELECT e FROM EmailOutbox e WHERE e.status = 'PENDING' "
            + "AND e.nextAttemptAt <= :agora ORDER BY e.createdAt ASC")
    List<EmailOutbox> proximosPendentes(@Param("agora") Instant agora, Pageable limite);

    long countByStatus(String status);
}
