package com.betobanco.email.entity;

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
@Table(name = "email_outbox")
public class EmailOutbox {

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";

    private static final Duration[] BACKOFF = {
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30),
            Duration.ofHours(2), Duration.ofHours(12)
    };

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(nullable = false)
    private String template;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private String status = PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected EmailOutbox() {
    }

    public EmailOutbox(String toAddress, String template, String payload, String dedupKey) {
        this.toAddress = toAddress;
        this.template = template;
        this.payload = payload;
        this.dedupKey = dedupKey;
    }

    public UUID getId() {
        return id;
    }

    public String getToAddress() {
        return toAddress;
    }

    public String getTemplate() {
        return template;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void marcarEnviado() {
        this.status = SENT;
        this.sentAt = Instant.now();
        this.errorMessage = null;
    }

    public void registrarFalha(String erro) {
        this.attempts++;
        this.errorMessage = erro == null ? "erro desconhecido"
                : erro.substring(0, Math.min(erro.length(), 1000));

        if (attempts >= BACKOFF.length) {
            this.status = FAILED;
        } else {
            this.status = PENDING;
            this.nextAttemptAt = Instant.now().plus(BACKOFF[attempts - 1]);
        }
    }
}
