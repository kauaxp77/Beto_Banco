package com.betobanco.webhooks.dto;

import com.betobanco.webhooks.entity.WebhookEvent;

import java.time.Instant;
import java.util.UUID;

/** A tela de webhooks mostra payload e erro: e a materia-prima do diagnostico. */
public record WebhookEventAdminResponse(
        UUID id,
        String provider,
        String eventId,
        String eventType,
        String status,
        int attempts,
        String errorMessage,
        String payload,
        Instant receivedAt,
        Instant processedAt) {

    public static WebhookEventAdminResponse from(WebhookEvent e) {
        return new WebhookEventAdminResponse(e.getId(), e.getProvider(), e.getEventId(),
                e.getEventType(), e.getStatus(), e.getAttempts(), e.getErrorMessage(),
                e.getPayload(), e.getReceivedAt(), e.getProcessedAt());
    }
}
