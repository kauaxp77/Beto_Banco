package com.betobanco.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_splits")
public class PaymentSplit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    private BigDecimal percentage;

    protected PaymentSplit() {
    }

    public PaymentSplit(UUID paymentId, String recipient, long amountCents,
                        BigDecimal percentage) {
        this.paymentId = paymentId;
        this.recipient = recipient;
        this.amountCents = amountCents;
        this.percentage = percentage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getRecipient() {
        return recipient;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}
