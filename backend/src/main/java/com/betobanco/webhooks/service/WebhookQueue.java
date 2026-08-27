package com.betobanco.webhooks.service;

import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Busca o proximo lote de eventos pendentes.
 *
 * <p>Vive em um bean separado do {@link WebhookProcessor} por um motivo
 * concreto: {@code @Transactional} so funciona atraves do proxy do Spring, e
 * uma chamada de um metodo da classe para outro da MESMA classe nao passa
 * pelo proxy. Com a busca aqui, a transacao existe de verdade — e sem ela a
 * query com {@code FOR UPDATE SKIP LOCKED} falha com "Query requires
 * transaction be in progress".
 */
@Component
public class WebhookQueue {

    private final WebhookEventRepository eventos;

    public WebhookQueue(WebhookEventRepository eventos) {
        this.eventos = eventos;
    }

    @Transactional
    public List<UUID> proximosIds(int limite) {
        return eventos.proximosPendentes(Instant.now(), PageRequest.of(0, limite))
                .stream().map(WebhookEvent::getId).toList();
    }
}
