package com.betobanco.email.service;

import com.betobanco.email.api.EmailService;
import com.betobanco.email.entity.EmailOutbox;
import com.betobanco.email.repository.EmailOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EmailOutboxService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);

    private final EmailOutboxRepository outbox;
    private final ObjectMapper mapper;

    public EmailOutboxService(EmailOutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public boolean enfileirar(String destinatario, String template, Map<String, Object> dados,
                              String dedupKey) {
        try {
            outbox.saveAndFlush(new EmailOutbox(
                    destinatario, template, mapper.writeValueAsString(dados), dedupKey));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Mesma dedupKey: a mensagem ja esta na fila ou ja foi enviada.
            log.debug("E-mail com dedupKey {} ja enfileirado", dedupKey);
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("nao foi possivel serializar o payload do e-mail", e);
        }
    }
}
