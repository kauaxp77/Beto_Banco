package com.betobanco.audit.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.audit.entity.AuditLog;
import com.betobanco.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditLoggerImpl implements AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggerImpl.class);

    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public AuditLoggerImpl(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void registrar(String acao, String tipoEntidade, String idEntidade,
                          Map<String, Object> metadados) {
        registrarComAtor(null, acao, tipoEntidade, idEntidade, metadados);
    }

    @Override
    @Transactional
    public void registrarComAtor(UUID atorId, String acao, String tipoEntidade,
                                 String idEntidade, Map<String, Object> metadados) {
        try {
            String json = metadados == null || metadados.isEmpty()
                    ? null : mapper.writeValueAsString(metadados);
            repo.saveAndFlush(new AuditLog(atorId, acao, tipoEntidade, idEntidade, json));
        } catch (Exception e) {
            // Auditoria nunca derruba a operacao auditada. Um log perdido e
            // ruim; uma venda perdida por causa dele seria pior.
            log.error("Falha ao gravar auditoria da acao {} sobre {} {}",
                    acao, tipoEntidade, idEntidade, e);
        }
    }
}
