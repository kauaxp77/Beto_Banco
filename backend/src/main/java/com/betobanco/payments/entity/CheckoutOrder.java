package com.betobanco.payments.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Pedido criado antes de mandar o comprador para a InfinitePay.
 * Documento Mestre Premium V3.0, secao 8.
 *
 * <p>O id desta linha vai como {@code order_nsu} e volta no webhook. E o unico
 * elo entre o pagamento aprovado e quem comprou o que: o webhook do Checkout
 * Integrado nao traz e-mail do comprador nem referencia ao produto.
 */
@Entity
@Table(name = "checkout_orders")
public class CheckoutOrder {

    public static final String CREATED = "CREATED";
    public static final String PAID = "PAID";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_phone")
    private String buyerPhone;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private String status = CREATED;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "invoice_slug")
    private String invoiceSlug;

    @Column(name = "transaction_nsu")
    private String transactionNsu;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected CheckoutOrder() {
    }

    public CheckoutOrder(UUID productId, String buyerEmail, String buyerName,
                         String buyerPhone, long amountCents) {
        this.productId = productId;
        this.buyerEmail = buyerEmail == null ? null : buyerEmail.trim().toLowerCase(Locale.ROOT);
        this.buyerName = buyerName;
        this.buyerPhone = buyerPhone;
        this.amountCents = amountCents;
    }

    public void registrarLink(String url, String slug) {
        this.checkoutUrl = url;
        if (slug != null && !slug.isBlank()) {
            this.invoiceSlug = slug;
        }
    }

    /**
     * Marca o pedido como pago.
     *
     * <p>Idempotente: o webhook da InfinitePay pode repetir, e o processador ja
     * trata reentrega. Marcar duas vezes nao pode mover {@code paidAt}, que e o
     * momento do pagamento e nao o da ultima notificacao — a conciliacao diaria
     * compara essa data.
     */
    public void marcarPago(String transactionNsu, String invoiceSlug) {
        if (transactionNsu == null || transactionNsu.isBlank()) {
            throw new IllegalArgumentException(
                    "Pagamento sem transaction_nsu não é conciliável nem confirmável na API");
        }
        if (PAID.equals(status)) {
            return;
        }
        this.transactionNsu = transactionNsu;
        if (invoiceSlug != null && !invoiceSlug.isBlank()) {
            this.invoiceSlug = invoiceSlug;
        }
        this.status = PAID;
        this.paidAt = Instant.now();
    }

    /** Pedido pago nao volta a aberto: o cancelamento depois disso e estorno. */
    public void cancelar() {
        if (PAID.equals(status)) {
            throw new IllegalStateException(
                    "Pedido já pago; o caminho aqui é estorno, não cancelamento.");
        }
        this.status = CANCELLED;
    }

    public boolean pago() {
        return PAID.equals(status);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getStatus() {
        return status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getInvoiceSlug() {
        return invoiceSlug;
    }

    public String getTransactionNsu() {
        return transactionNsu;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
