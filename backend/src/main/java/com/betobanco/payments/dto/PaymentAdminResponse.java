package com.betobanco.payments.dto;

import com.betobanco.payments.entity.Payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentAdminResponse(
        UUID id,
        String provider,
        String providerTransactionId,
        UUID productId,
        UUID userId,
        String buyerEmail,
        String buyerName,
        long amountCents,
        String currency,
        String status,
        Instant approvedAt,
        Instant createdAt) {

    public static PaymentAdminResponse from(Payment p) {
        return new PaymentAdminResponse(p.getId(), p.getProvider(),
                p.getProviderTransactionId(), p.getProductId(), p.getUserId(),
                p.getBuyerEmail(), p.getBuyerName(), p.getAmountCents(), p.getCurrency(),
                p.getStatus(), p.getApprovedAt(), p.getCreatedAt());
    }
}
