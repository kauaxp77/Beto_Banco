package com.betobanco.webhooks.service;

import com.betobanco.payments.api.PaymentGateway;
import com.betobanco.payments.api.PaymentNotification;
import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Estagio 1 do fluxo de pagamento: recepcao.
 *
 * <p>Valida a assinatura, grava o evento e sai. A idempotencia vem da
 * violacao da constraint unica {@code (provider, event_id)} — nao de um
 * "consulta se existe, senao insere", entre cujos dois passos cabe outra
 * requisicao concorrente. E assim que se cria aluno duplicado sob retry.
 */
@Service
public class WebhookIngestService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestService.class);

    public enum Desfecho {
        ACEITO, DUPLICADO, ASSINATURA_INVALIDA, PROVEDOR_DESCONHECIDO, ILEGIVEL
    }

    public record Resultado(Desfecho desfecho, String eventId) {
    }

    private final Map<String, PaymentGateway> gateways;
    private final WebhookEventRepository eventos;

    public WebhookIngestService(List<PaymentGateway> gateways, WebhookEventRepository eventos) {
        this.gateways = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::provider, Function.identity()));
        this.eventos = eventos;
    }

    /**
     * Sem {@code @Transactional} de proposito: a unica escrita e o insert do
     * evento, que roda na transacao do proprio repositorio. Uma transacao
     * externa seria marcada rollback-only pela colisao na constraint e o
     * commit falharia DEPOIS do catch — duplicado viraria 500.
     */
    public Resultado receber(String provider, byte[] corpoCru, Map<String, String> cabecalhos) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            return new Resultado(Desfecho.PROVEDOR_DESCONHECIDO, null);
        }

        // Assinatura invalida nao e persistida: gravar corpos nao autenticados
        // transformaria a tabela em alvo de enchimento por qualquer um que
        // descobrisse a URL.
        if (!gateway.assinaturaValida(corpoCru, cabecalhos)) {
            return new Resultado(Desfecho.ASSINATURA_INVALIDA, null);
        }

        Optional<PaymentNotification> interpretado = gateway.interpretar(corpoCru);
        if (interpretado.isEmpty()) {
            return new Resultado(Desfecho.ILEGIVEL, null);
        }
        PaymentNotification n = interpretado.get();

        try {
            eventos.saveAndFlush(new WebhookEvent(
                    provider, n.eventId(), n.eventType(),
                    new String(corpoCru, StandardCharsets.UTF_8)));
            return new Resultado(Desfecho.ACEITO, n.eventId());
        } catch (DataIntegrityViolationException e) {
            // Colidiu no indice unico: e o mesmo evento chegando de novo.
            // Retry do gateway e comportamento normal, nao erro.
            log.debug("Evento {} do provedor {} ja havia sido recebido", n.eventId(), provider);
            return new Resultado(Desfecho.DUPLICADO, n.eventId());
        }
    }
}
