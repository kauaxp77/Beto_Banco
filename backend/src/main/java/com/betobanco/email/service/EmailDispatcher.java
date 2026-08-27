package com.betobanco.email.service;

import com.betobanco.email.entity.EmailOutbox;
import com.betobanco.email.repository.EmailOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Estagio 3: entrega do e-mail.
 *
 * <p>Roda fora da transacao de dominio de proposito. A garantia aqui e
 * <b>at-least-once</b>: se o processo morrer entre o envio e a marcacao, o
 * destinatario recebe duas vezes. E o trade-off certo — nunca receber e muito
 * pior —, mas e uma propriedade declarada do desenho, nao um defeito.
 */
@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);
    private static final int LOTE = 20;

    private final EmailOutboxRepository outbox;
    private final EmailSender sender;
    private final ObjectMapper mapper;
    private final boolean habilitado;

    public EmailDispatcher(EmailOutboxRepository outbox, EmailSender sender, ObjectMapper mapper,
                           @Value("${betobanco.email.dispatcher-enabled:true}") boolean habilitado) {
        this.outbox = outbox;
        this.sender = sender;
        this.mapper = mapper;
        this.habilitado = habilitado;
    }

    /**
     * {@code @Transactional} AQUI, e nao so em processarLote: o scheduler
     * entra por este metodo atraves do proxy do Spring, e a chamada interna
     * a processarLote() (self-invocation) nao passa pelo proxy — sem a
     * transacao aberta aqui, o lock pessimista de proximosPendentes falha em
     * toda execucao agendada e a outbox fica parada para sempre.
     */
    @Scheduled(fixedDelayString = "${betobanco.email.dispatch-interval-ms:15000}")
    @Transactional
    public void despachar() {
        if (!habilitado) {
            return;
        }
        try {
            processarLote();
        } catch (Exception e) {
            log.error("Falha inesperada no despacho de e-mails", e);
        }
    }

    @Transactional
    public int processarLote() {
        List<EmailOutbox> pendentes =
                outbox.proximosPendentes(java.time.Instant.now(), PageRequest.of(0, LOTE));

        int enviados = 0;
        for (EmailOutbox mensagem : pendentes) {
            try {
                Map<String, Object> dados = mapper.readValue(mensagem.getPayload(), Map.class);
                sender.enviar(mensagem.getToAddress(), mensagem.getTemplate(), dados);
                mensagem.marcarEnviado();
                enviados++;
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail {} para {}: {}",
                        mensagem.getTemplate(), mensagem.getToAddress(), e.getMessage());
                mensagem.registrarFalha(e.getMessage());
            }
        }
        outbox.saveAll(pendentes);
        outbox.flush();
        return enviados;
    }
}
