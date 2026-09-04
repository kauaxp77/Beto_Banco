package com.betobanco.payments.service;

import com.betobanco.leads.api.LeadCapture;
import com.betobanco.payments.api.PaymentLedger;
import com.betobanco.payments.api.PaymentNotification;
import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.entity.PaymentSplit;
import com.betobanco.payments.entity.CheckoutOrder;
import com.betobanco.payments.repository.CheckoutOrderRepository;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.payments.repository.PaymentSplitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentLedgerService implements PaymentLedger {

    private final PaymentRepository pagamentos;
    private final PaymentSplitRepository splits;
    private final CheckoutOrderRepository pedidos;
    private final LeadCapture leads;

    public PaymentLedgerService(PaymentRepository pagamentos, PaymentSplitRepository splits,
                                CheckoutOrderRepository pedidos, LeadCapture leads) {
        this.pagamentos = pagamentos;
        this.splits = splits;
        this.pedidos = pedidos;
        this.leads = leads;
    }

    @Override
    @Transactional
    public Registro registrar(PaymentNotification n, String provider) {
        Comprador comprador = compradorDe(n);

        Payment pagamento = pagamentos
                .findByProviderAndProviderTransactionId(provider, n.transactionId())
                .orElseGet(() -> new Payment(provider, n.transactionId(), comprador.email(),
                        n.amountCents(), Payment.PENDING));

        pagamento.setBuyerName(comprador.nome());
        pagamento.setCurrency(n.currency());
        pagamentos.saveAndFlush(pagamento);

        if (!n.splits().isEmpty() && splits.countByPaymentId(pagamento.getId()) == 0) {
            for (PaymentNotification.Split s : n.splits()) {
                splits.save(new PaymentSplit(pagamento.getId(), s.recipient(),
                        s.amountCents(), s.percentage()));
            }
            splits.flush();
        }

        return new Registro(pagamento.getId(), pagamento.getUserId());
    }

    @Override
    @Transactional
    public void marcarAprovado(UUID paymentId, UUID userId, UUID productId) {
        Payment pagamento = exigir(paymentId);
        pagamento.setUserId(userId);
        pagamento.setProductId(productId);
        pagamento.setStatus(Payment.APPROVED);
        pagamento.setApprovedAt(Instant.now());
        pagamentos.saveAndFlush(pagamento);
    }

    @Override
    @Transactional
    public void marcarPendente(UUID paymentId) {
        mudarStatus(paymentId, Payment.PENDING);
    }

    /**
     * Documento Mestre Premium V3.0, secao 8: venda que nao se concretiza vira
     * lead de recuperacao, com nome, e-mail, curso, valor e motivo.
     *
     * <p>O motivo sai como CANCELADO, e nao RECUSADO, porque o contrato atual
     * de {@link PaymentNotification.Tipo} nao distingue os dois: o gateway
     * manda um unico evento de nao-conclusao. A distincao existe na porta e no
     * banco e entra sozinha quando a InfinityPay expuser qual foi o caso.
     */
    @Override
    @Transactional
    public void marcarCancelado(UUID paymentId) {
        mudarStatus(paymentId, Payment.CANCELLED);

        Payment pagamento = exigir(paymentId);
        leads.registrarVendaPerdida(new LeadCapture.VendaPerdida(
                pagamento.getBuyerName(),
                pagamento.getBuyerEmail(),
                pagamento.getProductId(),
                pagamento.getAmountCents(),
                LeadCapture.Motivo.CANCELADO,
                "Pagamento não concluído no gateway " + pagamento.getProvider()));
    }

    @Override
    @Transactional
    public void marcarEstornado(UUID paymentId, boolean chargeback) {
        mudarStatus(paymentId, chargeback ? Payment.CHARGEBACK : Payment.REFUNDED);
    }

    @Override
    @Transactional(readOnly = true)
    public Resumo resumo() {
        return new Resumo(pagamentos.countByStatus(Payment.APPROVED),
                pagamentos.somaAprovadaCents());
    }

    /**
     * Quem comprou.
     *
     * <p>O webhook do Checkout Integrado da InfinitePay nao traz e-mail do
     * comprador — traz {@code order_nsu}. Como {@code payments.buyer_email} e
     * obrigatorio (e precisa ser: e o registro fiscal da venda, e a conciliacao
     * diaria da secao 12 cruza por ele), o dado vem do nosso proprio pedido.
     *
     * <p>Isto e leitura de banco, nao chamada externa: nao viola a regra de nao
     * haver chamada de rede dentro da transacao de processamento.
     */
    private Comprador compradorDe(PaymentNotification n) {
        if (n.buyerEmail() != null && !n.buyerEmail().isBlank()) {
            return new Comprador(n.buyerEmail(), n.buyerName());
        }

        return pedidoDe(n)
                .map(p -> new Comprador(p.getBuyerEmail(), p.getBuyerName()))
                // Sem e-mail e sem pedido nao ha venda identificavel. Falhar
                // aqui manda o evento para a fila do administrador, que e melhor
                // do que gravar um pagamento sem dono.
                .orElseThrow(() -> new IllegalStateException(
                        "Pagamento sem e-mail do comprador e sem pedido conhecido "
                                + "(order_nsu=" + n.orderNsu() + ")."));
    }

    private Optional<CheckoutOrder> pedidoDe(PaymentNotification n) {
        if (n.orderNsu() != null && !n.orderNsu().isBlank()) {
            try {
                Optional<CheckoutOrder> achado =
                        pedidos.findById(UUID.fromString(n.orderNsu().trim()));
                if (achado.isPresent()) {
                    return achado;
                }
            } catch (IllegalArgumentException naoEhUuid) {
                // order_nsu de fatura criada fora da plataforma: cai no slug.
            }
        }
        if (n.invoiceSlug() != null && !n.invoiceSlug().isBlank()) {
            return pedidos.findByInvoiceSlug(n.invoiceSlug());
        }
        return Optional.empty();
    }

    private record Comprador(String email, String nome) {
    }

    private void mudarStatus(UUID paymentId, String status) {
        Payment pagamento = exigir(paymentId);
        pagamento.setStatus(status);
        pagamentos.saveAndFlush(pagamento);
    }

    private Payment exigir(UUID paymentId) {
        return pagamentos.findById(paymentId).orElseThrow(
                () -> new IllegalStateException("pagamento inexistente: " + paymentId));
    }
}
