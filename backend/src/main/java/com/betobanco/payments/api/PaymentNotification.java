package com.betobanco.payments.api;

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
        List<Split> splits) {

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
