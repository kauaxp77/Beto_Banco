package com.betobanco.privacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Prova de que uma pessoa aceitou uma versao especifica de um texto legal.
 *
 * <p>Secao 22 — os quatro elementos que tornam o aceite demonstravel: quem,
 * qual versao, quando e de onde. Faltando qualquer um deles, o registro nao
 * sustenta a alegacao de que houve consentimento informado.
 */
@Entity
@Table(name = "legal_acceptances")
public class LegalAcceptance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "accepted_at", insertable = false, updatable = false)
    private Instant acceptedAt;

    protected LegalAcceptance() {
    }

    public LegalAcceptance(UUID userId, UUID documentId, String ip, String userAgent) {
        this.userId = userId;
        this.documentId = documentId;
        this.ip = ip;
        // O user agent inteiro nao acrescenta nada e alguns navegadores mandam
        // strings enormes; 400 caracteres identificam o dispositivo de sobra.
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 400));
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
