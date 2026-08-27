package com.betobanco.webhooks.api;

/**
 * Contrato que o modulo {@code webhooks} publica para monitoramento.
 * O dashboard so precisa saber quantos eventos aguardam um humano.
 */
public interface WebhookMonitor {

    /** Eventos em FAILED ou MANUAL — a fila de atencao do administrador. */
    long aguardandoAtencao();
}
