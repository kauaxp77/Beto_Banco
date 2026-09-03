package com.betobanco.payments.api;

import java.time.Instant;
import java.util.List;

/**
 * Forma canonica de um evento de pagamento. E o unico vocabulario que o
 * dominio entende — o formato de cada provedor morre na fronteira do gateway.
 */
public record PaymentNotification(
        String eventId,
        String eventType,
        Tipo tipo,
        String transactionId,
        String sku,
        String buyerEmail,
        String buyerName,
        long amountCents,
        String currency,
        List<Split> splits,
        /**
         * Momento do evento no provedor, quando ele declara um.
         *
         * <p>Secao 12: "eventos podem chegar fora de ordem. Comparar
         * ocorrido_em e ignorar evento mais antigo que o estado atual". A
         * ordem de chegada aqui nao serve para isso: ela depende da fila e
         * das retentativas do gateway, nao de quando o fato aconteceu.
         * Nulo quando o provedor nao informa.
         */
        Instant occurredAt) {

    /**
     * As acoes que o RF-01 define por status. Cancelado e reembolso sao
     * eventos distintos porque ocorrem em momentos distintos: cancelamento
     * antes de o acesso existir, estorno depois.
     */
    public enum Tipo {
        APROVADO,
        PENDENTE,
        CANCELADO,
        REEMBOLSADO,
        CHARGEBACK,
        /** Evento legitimo, mas que o sistema nao precisa tratar. */
        IGNORADO
    }

    public record Split(String recipient, long amountCents, java.math.BigDecimal percentage) {
    }
}
