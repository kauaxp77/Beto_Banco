package com.betobanco.email.api;

import java.util.Map;

/**
 * Contrato que o modulo {@code email} publica.
 *
 * <p>Enfileirar NAO envia: grava na outbox e retorna. O envio acontece depois,
 * num worker separado, porque mensagem enviada nao tem rollback — se o envio
 * estivesse dentro da transacao de dominio, uma falha posterior desfaria o
 * aluno criado mas nao desfaria o e-mail que ja saiu.
 */
public interface EmailService {

    /**
     * @param dedupKey chave que impede a mesma mensagem de ser enfileirada
     *                 duas vezes. Enfileirar de novo com a mesma chave e
     *                 silenciosamente ignorado.
     * @return true se a mensagem foi enfileirada agora; false se ja existia.
     */
    boolean enfileirar(String destinatario, String template, Map<String, Object> dados,
                       String dedupKey);

    /** Templates conhecidos. O corpo de cada um vive no modulo de e-mail. */
    final class Templates {
        public static final String PRIMEIRO_ACESSO = "PRIMEIRO_ACESSO";
        public static final String ACESSO_LIBERADO = "ACESSO_LIBERADO";
        public static final String RECUPERACAO_SENHA = "RECUPERACAO_SENHA";
        public static final String ACESSO_REVOGADO = "ACESSO_REVOGADO";
        public static final String ANUNCIO = "ANUNCIO";

        private Templates() {
        }
    }
}
