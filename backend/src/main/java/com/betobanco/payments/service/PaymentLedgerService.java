package com.betobanco.payments.service;

import com.betobanco.leads.api.LeadCapture;
import com.betobanco.payments.api.PaymentLedger;
import com.betobanco.payments.api.PaymentNotification;
import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.entity.PaymentSplit;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.payments.repository.PaymentSplitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentLedgerService implements PaymentLedger {

    private final PaymentRepository pagamentos;
    private final PaymentSplitRepository splits;
    private final LeadCapture leads;

    public PaymentLedgerService(PaymentRepository pagamentos, PaymentSplitRepository splits,
                                LeadCapture leads) {
        this.pagamentos = pagamentos;
        this.splits = splits;
        this.leads = leads;
    }

    @Override
    @Transactional
    public Registro registrar(PaymentNotification n, String provider) {
        Payment pagamento = pagamentos
                .findByProviderAndProviderTransactionId(provider, n.transactionId())
                .orElseGet(() -> new Payment(provider, n.transactionId(), n.buyerEmail(),
                        n.amountCents(), Payment.PENDING));

        pagamento.setBuyerName(n.buyerName());
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
