package com.betobanco.privacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma decisao de consentimento, por finalidade. Secao 22.
 *
 * <p>A tabela e append-only: revogar grava uma linha nova com
 * {@code granted = false}, nunca atualiza a anterior. Sobrescrever apagaria a
 * evidencia de que houve consentimento antes — e e justamente essa evidencia
 * que precisa existir quando alguem questiona um envio passado.
 *
 * <p>Secao 16: "Caixa pre-marcada nao e consentimento valido." Por isso
 * {@code granted} e sempre um valor vindo explicitamente do titular, e a
 * ausencia de registro significa recusa, nunca aceite.
 */
@Entity
@Table(name = "consents")
public class Consent {

    public static final String MARKETING_WHATSAPP = "MARKETING_WHATSAPP";
    public static final String MARKETING_EMAIL = "MARKETING_EMAIL";
    public static final String COOKIE_ANALYTICS = "COOKIE_ANALYTICS";
    public static final String COOKIE_MARKETING = "COOKIE_MARKETING";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private boolean granted;

    /** O texto exato que a pessoa viu ao decidir. Sem ele o registro nao prova nada. */
    @Column(name = "accepted_text", nullable = false)
    private String acceptedText;

    @Column
    private String ip;

    @Column(name = "recorded_at", insertable = false, updatable = false)
    private Instant recordedAt;

    protected Consent() {
    }

    public Consent(UUID userId, String purpose, boolean granted, String acceptedText, String ip) {
        this.userId = userId;
        this.purpose = purpose;
        this.granted = granted;
        this.acceptedText = acceptedText;
        this.ip = ip;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPurpose() {
        return purpose;
    }

    public boolean isGranted() {
        return granted;
    }

    public String getAcceptedText() {
        return acceptedText;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
