package com.betobanco.webhooks.repository;

import com.betobanco.webhooks.entity.WebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
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

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByProviderAndEventId(String provider, String eventId);

    Page<WebhookEvent> findByStatusInOrderByReceivedAtDesc(List<String> status, Pageable pageable);

    Page<WebhookEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);

    long countByStatusIn(List<String> status);

    /**
     * Pega o proximo lote de eventos pendentes com {@code FOR UPDATE SKIP
     * LOCKED}: se um dia o backend rodar em duas instancias, elas nao brigam
     * pelo mesmo evento nem o processam em duplicidade.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout",
            value = "-2"))
    @Query("SELECT e FROM WebhookEvent e WHERE e.status = 'RECEIVED' "
            + "OR (e.status = 'FAILED' AND e.nextAttemptAt <= :agora) "
            // Secao 12: a ordem e a do provedor (occurredAt), nao a de chegada.
            // Um cancelamento que ficou preso na fila do gateway chega depois da
            // aprovacao que ele desfaz; drenar por receivedAt aplicaria os dois
            // na ordem errada e deixaria o aluno com acesso a um pedido estornado.
            // receivedAt segue como desempate para quem nao declara momento.
            + "ORDER BY e.occurredAt ASC NULLS FIRST, e.receivedAt ASC")
    List<WebhookEvent> proximosPendentes(@Param("agora") Instant agora, Pageable limite);
}
