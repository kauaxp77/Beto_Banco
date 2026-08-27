package com.betobanco.webhooks;

import com.betobanco.audit.repository.AuditLogRepository;
import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.email.entity.EmailOutbox;
import com.betobanco.email.repository.EmailOutboxRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.gateway.FakePaymentGateway;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.payments.repository.PaymentSplitRepository;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import com.betobanco.webhooks.service.WebhookProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o caminho pagamento -> acesso de ponta a ponta, incluindo os cenarios
 * que o documento de requisitos exige: TESTE 01 (pagamento aprovado libera o
 * aluno) e TESTE 02 (webhook duplicado nao duplica).
 */
@AutoConfigureMockMvc
class WebhookFluxoTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakePaymentGateway gateway;

    @Autowired
    private WebhookProcessor processor;

    @Autowired
    private WebhookEventRepository eventos;

    @Autowired
    private PaymentRepository pagamentos;

    @Autowired
    private PaymentSplitRepository splits;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private UserDirectory usuarios;

    @Autowired
    private EntitlementService entitlements;

    @Autowired
    private EmailOutboxRepository outbox;

    @Autowired
    private AuditLogRepository auditoria;

    private UUID criarProduto(String sku) {
        return produtos.saveAndFlush(new Product(sku, "Mentoria " + sku, null, 19900L)).getId();
    }

    private String payload(String eventId, String evento, String tx, String sku, String email) {
        return """
                {"event_id":"%s","event":"%s","transaction_id":"%s","sku":"%s",
                 "buyer_email":"%s","buyer_name":"Comprador Teste","amount_cents":19900,
                 "currency":"BRL","splits":[{"recipient":"produtor","amount_cents":15920,
                 "percentage":"80.00"},{"recipient":"plataforma","amount_cents":3980,
                 "percentage":"20.00"}]}""".formatted(eventId, evento, tx, sku, email);
    }

    private int enviar(String corpo) throws Exception {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        return mockMvc.perform(post("/webhooks/payment/fake")
                        .header(FakePaymentGateway.CABECALHO_ASSINATURA, gateway.assinar(bytes))
                        .contentType("application/json").content(bytes))
                .andReturn().getResponse().getStatus();
    }

    // ---------- TESTE 01 ----------

    @Test
    void pagamentoAprovadoCriaAlunoLiberaAcessoEEnfileiraEmail() throws Exception {
        criarProduto("SKU-T01");
        String corpo = payload("evt-t01", "payment.approved", "tx-t01", "SKU-T01",
                "novo@aluno.com");

        assertThat(enviar(corpo)).isEqualTo(200);
        assertThat(processor.processarLote()).isPositive();

        // Aluno criado, SEM senha: ele recebe link de definicao, nao senha.
        var aluno = usuarios.buscarPorEmail("novo@aluno.com").orElseThrow();

        // Acesso liberado.
        var produto = produtos.findBySku("SKU-T01").orElseThrow();
        assertThat(entitlements.temAcesso(aluno.id(), produto.getId())).isTrue();

        // Pagamento registrado como aprovado, com os splits.
        Payment pago = pagamentos.findByProviderAndProviderTransactionId("fake", "tx-t01")
                .orElseThrow();
        assertThat(pago.getStatus()).isEqualTo(Payment.APPROVED);
        assertThat(pago.getApprovedAt()).isNotNull();
        assertThat(splits.findByPaymentId(pago.getId())).hasSize(2);

        // E-mail de primeiro acesso na outbox — enfileirado, nao enviado.
        assertThat(outbox.findByDedupKey("primeiro-acesso:" + aluno.id())).isPresent();

        // Auditoria dos dois fatos.
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("PAYMENT_APPROVED")).isNotEmpty();
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("ACCESS_GRANTED")).isNotEmpty();

        WebhookEvent evento = eventos.findByProviderAndEventId("fake", "evt-t01").orElseThrow();
        assertThat(evento.getStatus()).isEqualTo(WebhookEvent.PROCESSED);
    }

    // ---------- TESTE 02 ----------

    @Test
    void webhookDuplicadoNaoDuplicaNadaEDevolve200() throws Exception {
        criarProduto("SKU-T02");
        String corpo = payload("evt-t02", "payment.approved", "tx-t02", "SKU-T02",
                "dup@aluno.com");

        assertThat(enviar(corpo)).isEqualTo(200);
        assertThat(enviar(corpo)).isEqualTo(200); // retry do gateway e normal
        assertThat(enviar(corpo)).isEqualTo(200);

        processor.processarLote();

        assertThat(eventos.findAll().stream()
                .filter(e -> "evt-t02".equals(e.getEventId())).count()).isEqualTo(1);

        var aluno = usuarios.buscarPorEmail("dup@aluno.com").orElseThrow();
        assertThat(entitlements.listarDe(aluno.id())).hasSize(1);
    }

    @Test
    void webhookDuplicadoCONCORRENTECriaUmUnicoAluno() throws Exception {
        criarProduto("SKU-CONC");
        String corpo = payload("evt-conc", "payment.approved", "tx-conc", "SKU-CONC",
                "conc@aluno.com");
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        String assinatura = gateway.assinar(bytes);

        // Um "if (existe) return" ingenuo PASSA no teste sequencial acima e
        // ainda assim duplica aqui. So a unique constraint segura isto.
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    largada.await();
                    mockMvc.perform(post("/webhooks/payment/fake")
                            .header(FakePaymentGateway.CABECALHO_ASSINATURA, assinatura)
                            .contentType("application/json").content(bytes));
                    ok.incrementAndGet();
                } catch (Exception ignored) {
                    // Colisao no indice unico e o comportamento esperado.
                }
            });
        }
        largada.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(eventos.findAll().stream()
                .filter(e -> "evt-conc".equals(e.getEventId())).count()).isEqualTo(1);
    }

    // ---------- seguranca ----------

    @Test
    void assinaturaInvalidaDevolve401ENaoPersisteNada() throws Exception {
        String corpo = payload("evt-forjado", "payment.approved", "tx-forjado", "SKU-X",
                "invasor@aluno.com");

        mockMvc.perform(post("/webhooks/payment/fake")
                        .header(FakePaymentGateway.CABECALHO_ASSINATURA, "assinatura-inventada")
                        .contentType("application/json").content(corpo))
                .andExpect(status().isUnauthorized());

        assertThat(eventos.findByProviderAndEventId("fake", "evt-forjado")).isEmpty();
    }

    @Test
    void provedorDesconhecidoDevolve404() throws Exception {
        mockMvc.perform(post("/webhooks/payment/provedor-que-nao-existe")
                        .header(FakePaymentGateway.CABECALHO_ASSINATURA, "x")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
    }

    // ---------- regras de negocio por status ----------

    @Test
    void skuDesconhecidoFalhaSemCriarAlunoEVaiParaAFilaDoAdmin() throws Exception {
        String corpo = payload("evt-sku", "payment.approved", "tx-sku", "SKU-INEXISTENTE",
                "semproduto@aluno.com");

        assertThat(enviar(corpo)).isEqualTo(200);
        processor.processarLote();

        WebhookEvent evento = eventos.findByProviderAndEventId("fake", "evt-sku").orElseThrow();
        assertThat(evento.getStatus()).isEqualTo(WebhookEvent.FAILED);
        assertThat(evento.getErrorMessage()).contains("SKU desconhecido");
        assertThat(evento.getAttempts()).isEqualTo(1);

        // Adivinhar qual produto liberar seria pior do que nao liberar.
        assertThat(usuarios.buscarPorEmail("semproduto@aluno.com")).isEmpty();
    }

    @Test
    void pagamentoPendenteNaoLiberaAcesso() throws Exception {
        criarProduto("SKU-PEND");
        assertThat(enviar(payload("evt-pend", "payment.pending", "tx-pend", "SKU-PEND",
                "pendente@aluno.com"))).isEqualTo(200);
        processor.processarLote();

        Payment pago = pagamentos.findByProviderAndProviderTransactionId("fake", "tx-pend")
                .orElseThrow();
        assertThat(pago.getStatus()).isEqualTo(Payment.PENDING);
        assertThat(usuarios.buscarPorEmail("pendente@aluno.com")).isEmpty();
    }

    @Test
    void estornoRevogaOAcessoLiberado() throws Exception {
        criarProduto("SKU-EST");
        assertThat(enviar(payload("evt-est-1", "payment.approved", "tx-est", "SKU-EST",
                "estorno@aluno.com"))).isEqualTo(200);
        processor.processarLote();

        var aluno = usuarios.buscarPorEmail("estorno@aluno.com").orElseThrow();
        var produto = produtos.findBySku("SKU-EST").orElseThrow();
        assertThat(entitlements.temAcesso(aluno.id(), produto.getId())).isTrue();

        // Sem isto, quem pede reembolso recebe o dinheiro E mantem o acesso.
        assertThat(enviar(payload("evt-est-2", "payment.refunded", "tx-est", "SKU-EST",
                "estorno@aluno.com"))).isEqualTo(200);
        processor.processarLote();

        assertThat(entitlements.temAcesso(aluno.id(), produto.getId())).isFalse();
        Payment pago = pagamentos.findByProviderAndProviderTransactionId("fake", "tx-est")
                .orElseThrow();
        assertThat(pago.getStatus()).isEqualTo(Payment.REFUNDED);
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("ACCESS_REVOKED")).isNotEmpty();
    }

    @Test
    void eventoSemEfeitoNoDominioEhIgnoradoSemFalhar() throws Exception {
        assertThat(enviar(payload("evt-ruido", "payment.whatever", "tx-ruido", "SKU-X",
                "ruido@aluno.com"))).isEqualTo(200);
        processor.processarLote();

        WebhookEvent evento = eventos.findByProviderAndEventId("fake", "evt-ruido").orElseThrow();
        assertThat(evento.getStatus()).isEqualTo(WebhookEvent.IGNORED);
    }

    @Test
    void alunoQueJaExisteRecebeEmailDeConteudoLiberadoENaoDePrimeiroAcesso() throws Exception {
        criarProduto("SKU-EXIST");
        usuarios.registrar("jaexiste@aluno.com", "senha-forte-123", "Ja Existe");

        assertThat(enviar(payload("evt-exist", "payment.approved", "tx-exist", "SKU-EXIST",
                "jaexiste@aluno.com"))).isEqualTo(200);
        processor.processarLote();

        var aluno = usuarios.buscarPorEmail("jaexiste@aluno.com").orElseThrow();
        assertThat(outbox.findByDedupKey("primeiro-acesso:" + aluno.id())).isEmpty();
        assertThat(outbox.findAll().stream()
                .anyMatch(e -> e.getTemplate().equals("ACESSO_LIBERADO"))).isTrue();
    }

    @Test
    void aOutboxNaoEnfileiraDuasVezesAMesmaMensagem() throws Exception {
        criarProduto("SKU-OUT");
        assertThat(enviar(payload("evt-out-1", "payment.approved", "tx-out", "SKU-OUT",
                "outbox@aluno.com"))).isEqualTo(200);
        processor.processarLote();

        // Reprocessar o mesmo pagamento nao gera segundo e-mail.
        WebhookEvent evento = eventos.findByProviderAndEventId("fake", "evt-out-1").orElseThrow();
        evento.reenfileirar();
        eventos.saveAndFlush(evento);
        processor.processarLote();

        var aluno = usuarios.buscarPorEmail("outbox@aluno.com").orElseThrow();
        long quantas = outbox.findAll().stream()
                .filter(e -> e.getDedupKey().equals("primeiro-acesso:" + aluno.id()))
                .count();
        assertThat(quantas).isEqualTo(1);
        assertThat(outbox.findByDedupKey("primeiro-acesso:" + aluno.id()))
                .map(EmailOutbox::getStatus).contains(EmailOutbox.PENDING);
    }
}
