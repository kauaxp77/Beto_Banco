package com.betobanco.leads;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.leads.entity.Lead;
import com.betobanco.leads.entity.LeadEvent;
import com.betobanco.leads.repository.LeadEventRepository;
import com.betobanco.leads.repository.LeadRepository;
import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.gateway.FakePaymentGateway;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.shared.tenant.TenantContext;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.webhooks.service.WebhookProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Documento Mestre Premium V3.0, secao 8: "Recusado. Criar lead." e "Registrar
 * automaticamente: nome, e-mail, WhatsApp, curso, valor, motivo."
 *
 * <p>A venda que nao se concretiza e o unico momento em que a plataforma sabe
 * quem quis comprar e nao comprou. Se esse contato nao for gravado ali, ele
 * nao existe em lugar nenhum depois.
 */
@AutoConfigureMockMvc
class RecuperacaoDeVendaTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakePaymentGateway gateway;

    @Autowired
    private WebhookProcessor processor;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private PaymentRepository pagamentos;

    @Autowired
    private LeadRepository leads;

    @Autowired
    private LeadEventRepository eventos;

    @Test
    void pagamentoCanceladoViraLeadComCursoValorEMotivo() throws Exception {
        produtos.saveAndFlush(new Product("SKU-LEAD", "Mentoria Bancária", null, 356400L));

        enviar(payload("evt-lead-01", "payment.cancelled", "tx-lead-01",
                "SKU-LEAD", "quase@comprou.com"));
        processor.processarLote();

        Payment pagamento = pagamentos
                .findByProviderAndProviderTransactionId("fake", "tx-lead-01").orElseThrow();
        assertThat(pagamento.getStatus()).isEqualTo(Payment.CANCELLED);

        Lead lead = leads.findByTenantIdAndEmail(TenantContext.RAIZ, "quase@comprou.com")
                .orElseThrow(() -> new AssertionError("A venda perdida não virou lead."));
        assertThat(lead.getName()).isEqualTo("Comprador Teste");
        assertThat(lead.getStatus()).isEqualTo(Lead.NEW);

        List<LeadEvent> historico = eventos.findByLeadIdOrderByOccurredAtDesc(lead.getId());
        assertThat(historico).hasSize(1);

        LeadEvent evento = historico.get(0);
        assertThat(evento.getSource()).isEqualTo(LeadEvent.PAGAMENTO_CANCELADO);
        // O valor e o que ordena a fila de quem ligar primeiro.
        assertThat(evento.getAmountCents()).isEqualTo(356400L);
        assertThat(evento.getReason()).contains("fake");
    }

    @Test
    void pagamentoAprovadoNaoGeraLead() throws Exception {
        // Quem comprou vira aluno, nao lead. Um lead aqui poria o cliente na
        // fila de recuperacao de venda, e alguem ligaria para vender o que ele
        // acabou de pagar.
        produtos.saveAndFlush(new Product("SKU-OK", "Mentoria Aprovada", null, 19900L));

        enviar(payload("evt-lead-02", "payment.approved", "tx-lead-02",
                "SKU-OK", "comprou@mesmo.com"));
        processor.processarLote();

        assertThat(leads.findByTenantIdAndEmail(TenantContext.RAIZ, "comprou@mesmo.com"))
                .isEmpty();
    }

    // ------------------------------------------------------------------

    private String payload(String eventId, String evento, String tx, String sku, String email) {
        return """
                {"event_id":"%s","event":"%s","transaction_id":"%s","sku":"%s",
                 "buyer_email":"%s","buyer_name":"Comprador Teste","amount_cents":356400,
                 "currency":"BRL","splits":[]}""".formatted(eventId, evento, tx, sku, email);
    }

    private void enviar(String corpo) throws Exception {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/webhooks/payment/fake")
                .header(FakePaymentGateway.CABECALHO_ASSINATURA, gateway.assinar(bytes))
                .contentType("application/json").content(bytes));
    }
}
