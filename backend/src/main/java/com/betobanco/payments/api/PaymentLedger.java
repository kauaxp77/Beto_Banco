package com.betobanco.payments.api;

import java.util.UUID;

/**
 * Contrato que o modulo {@code payments} publica para quem precisa registrar
 * o efeito financeiro de uma notificacao — hoje, o processador de webhooks.
 * Nenhum outro modulo conhece a tabela {@code payments}; daqui saem apenas
 * identificadores, nunca a entidade.
 */
public interface PaymentLedger {

    /**
     * Registra (ou reencontra) o pagamento da notificacao, idempotente por
     * {@code (provider, transactionId)}. Os splits sao gravados apenas na
     * primeira vez; reprocessar o mesmo evento nao os duplica.
     */
    Registro registrar(PaymentNotification notificacao, String provider);

    /** Aprova o pagamento e o vincula ao aluno e ao produto liberados. */
    void marcarAprovado(UUID paymentId, UUID userId, UUID productId);

    void marcarPendente(UUID paymentId);

    void marcarCancelado(UUID paymentId);

    /** Estorno: {@code REFUNDED}, ou {@code CHARGEBACK} quando contestado. */
    void marcarEstornado(UUID paymentId, boolean chargeback);

    /** Numeros agregados para o dashboard do admin. */
    Resumo resumo();

    /**
     * O que o resto do sistema pode saber de um pagamento registrado.
     * {@code userId} e nulo enquanto nenhuma aprovacao vinculou o pagamento
     * a um aluno — e o caso de um estorno que chega antes da aprovacao.
     */
    record Registro(UUID paymentId, UUID userId) {
    }

    record Resumo(long aprovados, long receitaAprovadaCents) {
    }
}
