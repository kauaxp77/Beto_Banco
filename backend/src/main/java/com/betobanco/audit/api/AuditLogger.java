package com.betobanco.audit.api;

import java.util.Map;
import java.util.UUID;

/**
 * Contrato que o modulo {@code audit} publica.
 *
 * <p>Registrar auditoria nunca pode derrubar a operacao auditada: uma falha
 * ao gravar o log e engolida e reportada, e nao propagada.
 */
public interface AuditLogger {

    /** Registra uma acao do sistema, sem ator humano identificado. */
    void registrar(String acao, String tipoEntidade, String idEntidade,
                   Map<String, Object> metadados);

    /** Registra uma acao executada por um usuario identificado. */
    void registrarComAtor(UUID atorId, String acao, String tipoEntidade, String idEntidade,
                          Map<String, Object> metadados);

    final class Acoes {
        public static final String LOGIN = "LOGIN";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String PASSWORD_RESET = "PASSWORD_RESET";
        public static final String PAYMENT_APPROVED = "PAYMENT_APPROVED";
        public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
        public static final String PAYMENT_CANCELLED = "PAYMENT_CANCELLED";
        public static final String ACCESS_GRANTED = "ACCESS_GRANTED";
        public static final String ACCESS_REVOKED = "ACCESS_REVOKED";
        public static final String STUDENT_BLOCKED = "STUDENT_BLOCKED";
        public static final String STUDENT_UNBLOCKED = "STUDENT_UNBLOCKED";
        public static final String WEBHOOK_REPROCESSED = "WEBHOOK_REPROCESSED";
        public static final String WEBHOOK_RESOLVED_MANUALLY = "WEBHOOK_RESOLVED_MANUALLY";
        public static final String ADMIN_ACTION = "ADMIN_ACTION";

        private Acoes() {
        }
    }
}
