package com.betobanco.leads.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Cada vez que o lead apareceu. Append-only.
 *
 * <p>V3.0, secao 8: "Recusado. Criar lead." e "Registrar automaticamente: nome,
 * e-mail, WhatsApp, curso, valor, motivo". O motivo e o valor sao o que decide
 * quem a equipe liga primeiro — um cartao recusado de R$ 3.564 hoje vale mais
 * que um PDF baixado no mes passado, e sobrescrever perderia essa diferenca.
 */
@Entity
@Table(name = "lead_events")
public class LeadEvent {

    public static final String MATERIAL = "MATERIAL";
    public static final String PAGAMENTO_RECUSADO = "PAGAMENTO_RECUSADO";
    public static final String PAGAMENTO_CANCELADO = "PAGAMENTO_CANCELADO";
    public static final String MANUAL = "MANUAL";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(nullable = false)
    private String source;

    @Column(name = "magnet_id")
    private UUID magnetId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "amount_cents")
    private Long amountCents;

    @Column
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected LeadEvent() {
    }

    public LeadEvent(UUID leadId, String source) {
        this.leadId = leadId;
        this.source = source;
    }

    public LeadEvent comMaterial(UUID magnetId) {
        this.magnetId = magnetId;
        return this;
    }

    public LeadEvent comVendaPerdida(UUID productId, Long amountCents, String motivo) {
        this.productId = productId;
        this.amountCents = amountCents;
        this.reason = motivo;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public String getSource() {
        return source;
    }

    public UUID getMagnetId() {
        return magnetId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
