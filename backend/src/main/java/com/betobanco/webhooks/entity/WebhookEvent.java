package com.betobanco.webhooks.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    public static final String RECEIVED = "RECEIVED";
    public static final String PROCESSING = "PROCESSING";
    public static final String PROCESSED = "PROCESSED";
    public static final String FAILED = "FAILED";
    public static final String IGNORED = "IGNORED";
    public static final String MANUAL = "MANUAL";

    /** Backoff exponencial: 1min, 5min, 15min, 1h, 6h. */
    private static final Duration[] BACKOFF = {
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15),
            Duration.ofHours(1), Duration.ofHours(6)
    };

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid = true;

    /**
     * Momento do evento no provedor (secao 12). Ordena a fila; received_at nao
     * serve, porque depende da fila e das retentativas do proprio gateway.
     * Nulo quando o provedor nao declara um.
     */
    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at", insertable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false)
    private String status = RECEIVED;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "error_message")
    private String errorMessage;

    protected WebhookEvent() {
    }

    public WebhookEvent(String provider, String eventId, String eventType, String payload) {
        this(provider, eventId, eventType, payload, null);
    }

    public WebhookEvent(String provider, String eventId, String eventType, String payload,
                        Instant occurredAt) {
        this.provider = provider;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void marcarProcessando() {
        this.status = PROCESSING;
    }

    public void marcarProcessado() {
        this.status = PROCESSED;
        this.processedAt = Instant.now();
        this.errorMessage = null;
        this.nextAttemptAt = null;
    }

    public void marcarIgnorado(String motivo) {
        this.status = IGNORED;
        this.processedAt = Instant.now();
        this.errorMessage = motivo;
        this.nextAttemptAt = null;
    }

    /**
     * Registra a falha e agenda a proxima tentativa. Esgotado o backoff, o
     * evento vai para MANUAL — que e o que alimenta a fila do administrador,
     * conforme o fluxo de excecao 2a do caso de uso.
     */
    public void registrarFalha(String erro) {
        this.attempts++;
        this.errorMessage = erro == null ? "erro desconhecido"
                : erro.substring(0, Math.min(erro.length(), 1000));

        if (attempts >= BACKOFF.length) {
            this.status = MANUAL;
            this.nextAttemptAt = null;
        } else {
            this.status = FAILED;
            this.nextAttemptAt = Instant.now().plus(BACKOFF[attempts - 1]);
        }
    }

    /** Devolve o evento para a fila, usado pela acao de reprocessar do admin. */
    public void reenfileirar() {
        this.status = RECEIVED;
        this.nextAttemptAt = null;
        this.errorMessage = null;
    }
}
