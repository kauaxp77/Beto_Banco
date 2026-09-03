package com.betobanco.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent")
    private String userAgent;

    private String ip;

    protected RefreshToken() {
    }

    /**
     * Secao 10 -- as colunas ip e user_agent existem no schema desde a V4 mas
     * nunca eram preenchidas. Sem elas nao da para limitar dispositivos nem
     * detectar conta compartilhada, que sao as duas mitigacoes que a secao 30
     * lista contra pirataria.
     */
    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt,
                        String ip, String userAgent) {
        this(userId, tokenHash, expiresAt);
        this.ip = ip;
        this.userAgent = userAgent == null ? null
                : userAgent.substring(0, Math.min(userAgent.length(), 400));
    }

    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revogar() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(UUID replacedBy) {
        this.replacedBy = replacedBy;
    }

    /** Ja foi usado para gerar outro token: reaparecer aqui e sinal de copia. */
    public boolean foiRotacionado() {
        return replacedBy != null;
    }

    public boolean estaVigente() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }
}
