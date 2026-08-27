package com.betobanco.entitlements.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entitlements")
public class Entitlement {

    public static final String FONTE_PAGAMENTO = "PAYMENT";
    public static final String FONTE_MANUAL = "MANUAL";
    public static final String FONTE_MIGRACAO = "MIGRATION";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String source;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "granted_at", insertable = false, updatable = false)
    private Instant grantedAt;

    /** Nulo significa vitalicio. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    protected Entitlement() {
    }

    public Entitlement(UUID userId, UUID productId, String source, String sourceRef) {
        this.userId = userId;
        this.productId = productId;
        this.source = source;
        this.sourceRef = sourceRef;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSource() {
        return source;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revogar() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    public boolean estaVigente() {
        return revokedAt == null && (expiresAt == null || Instant.now().isBefore(expiresAt));
    }
}
