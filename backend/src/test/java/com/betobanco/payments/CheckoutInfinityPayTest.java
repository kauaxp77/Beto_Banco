package com.betobanco.payments;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.payments.api.CheckoutGateway;
import com.betobanco.payments.entity.CheckoutOrder;
import com.betobanco.payments.gateway.InfinityPayGateway;
import com.betobanco.payments.repository.CheckoutOrderRepository;
import com.betobanco.payments.service.CheckoutService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.webhooks.service.WebhookProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Documento Mestre Premium V3.0, secao 8 — "Aluno escolhe curso -> Checkout ->
 * InfinityPay -> Webhook -> Criacao de usuario -> Liberacao automatica".
 *
 * <p>A regra que estes testes existem para proteger e uma so: <b>so recebe o
 * curso quem pagou por ele</b>. E ela e verificada contra o provedor, nao
 * contra o que o webhook afirma — o Checkout Integrado nao documenta assinatura
 * no corpo que envia.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "betobanco.payments.infinitypay.confirmar-antes-de-liberar=true",
        // Sem segredo o gateway recusa todo webhook, que e o comportamento
        // correto em producao; aqui ele precisa existir para o teste passar
        // pelo provedor REAL, e nao pelo de referencia.
        "betobanco.payments.infinitypay-webhook-secret=segredo-de-teste-com-mais-de-32-bytes"})
class CheckoutInfinityPayTest extends PostgresTestBase {

    private static final long PRECO = 356400L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CheckoutService checkout;

    @Autowired
    private CheckoutOrderRepository pedidos;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private InfinityPayGateway infinityPay;

    @Autowired
    private WebhookProcessor processor;

    @Autowired
    private UserDirectory usuarios;

    @Autowired
    private EntitlementService entitlements;

    @MockitoBean
    private CheckoutGateway gateway;

    private UUID produtoId;

    /**
     * E-mail novo a cada teste. O banco e compartilhado dentro da classe, e o
     * aluno criado pelo caminho feliz faria os testes negativos encontrarem uma
     * conta que eles proprios nao criaram — passando a mentir sobre o que
     * verificam.
     */
    private String email;

    @BeforeEach
    void prepararProdutoELink() {
        email = "compradora+" + UUID.randomUUID() + "@exemplo.com";

        produtoId = produtos
                .saveAndFlush(new Product("SKU-CHK-" + UUID.randomUUID(), "Mentoria Bancária",
                        null, PRECO))
                .getId();

        when(gateway.criarLink(any())).thenReturn(new CheckoutGateway.LinkDeCheckout(
                "https://checkout.infinitepay.com.br/betobanco?lenc=abc", null));
    }

    @Test
    @DisplayName("O pedido guarda o preço do catálogo, não o que o cliente mandar")
    void precoVemDoCatalogo() {
        CheckoutOrder pedido = checkout.abrir(produtoId, email, "Compradora Teste", null);

        assertThat(pedido.getAmountCents()).isEqualTo(PRECO);
        assertThat(pedido.getStatus()).isEqualTo(CheckoutOrder.CREATED);
        assertThat(pedido.getCheckoutUrl()).contains("checkout.infinitepay.com.br");
        // E-mail normalizado: o indice e a busca do webhook dependem disso.
        assertThat(pedido.getBuyerEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Pagamento confirmado pelo provedor libera o acesso")
    void pagamentoConfirmadoLiberaAcesso() throws Exception {
        CheckoutOrder pedido = checkout.abrir(produtoId, email, "Compradora Teste", null);
        confirmarComoPago(PRECO);

        enviarWebhook(pedido.getId().toString(), "tx-ok-1", "fatura-ok-1");
        processor.processarLote();

        var aluno = usuarios.buscarPorEmail(email).orElseThrow(
                () -> new AssertionError("O pagamento aprovado não criou o aluno."));
        assertThat(entitlements.temAcesso(aluno.id(), produtoId)).isTrue();
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().pago()).isTrue();
    }

    @Test
    @DisplayName("Provedor diz que NÃO está pago: nenhum acesso")
    void semPagamentoNaoHaAcesso() throws Exception {
        CheckoutOrder pedido = checkout.abrir(produtoId, email, "Compradora Teste", null);
        when(gateway.confirmar(anyString(), anyString(), any()))
                .thenReturn(new CheckoutGateway.Confirmacao(false, 0, 0, 0, null));

        enviarWebhook(pedido.getId().toString(), "tx-nao-pago", "fatura-nao-pago");
        processor.processarLote();

        assertThat(usuarios.buscarPorEmail(email)).isEmpty();
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().pago()).isFalse();
    }

    @Test
    @DisplayName("Pagou menos que o pedido cobrava: nenhum acesso")
    void pagamentoMenorQueOPedidoNaoLibera() throws Exception {
        // Sem esta verificacao, um webhook forjado poderia declarar qualquer
        // valor e a mentoria de R$ 3.564 sairia por um real.
        CheckoutOrder pedido = checkout.abrir(produtoId, email, "Compradora Teste", null);
        confirmarComoPago(100L);

        enviarWebhook(pedido.getId().toString(), "tx-barato", "fatura-barato");
        processor.processarLote();

        assertThat(usuarios.buscarPorEmail(email)).isEmpty();
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().pago()).isFalse();
    }

    @Test
    @DisplayName("Webhook citando pedido que não existe: nenhum acesso")
    void pedidoDesconhecidoNaoLibera() throws Exception {
        confirmarComoPago(PRECO);

        enviarWebhook(UUID.randomUUID().toString(), "tx-fantasma", "fatura-fantasma");
        processor.processarLote();

        assertThat(usuarios.buscarPorEmail(email)).isEmpty();
    }

    // ------------------------------------------------------------------

    private void confirmarComoPago(long valorCentavos) {
        when(gateway.confirmar(anyString(), anyString(), any()))
                .thenReturn(new CheckoutGateway.Confirmacao(
                        true, valorCentavos, valorCentavos, 1, "credit_card"));
    }

    /**
     * O corpo e exatamente o que a InfinitePay documenta para o Checkout
     * Integrado: sem campo de evento, sem e-mail do comprador e sem SKU. Quem
     * diz o que liberar e o {@code order_nsu}.
     *
     * <p>Vai pelo provedor REAL, com a assinatura real. Passar pelo gateway de
     * referencia esconderia justamente o que estes testes existem para cobrir:
     * ele le {@code transaction_id}, e a InfinitePay manda {@code transaction_nsu}.
     */
    private void enviarWebhook(String orderNsu, String transactionNsu, String slug)
            throws Exception {
        String corpo = """
                {"invoice_slug":"%s","amount":%d,"paid_amount":%d,"installments":1,
                 "capture_method":"credit_card","transaction_nsu":"%s","order_nsu":"%s",
                 "receipt_url":"https://comprovante.exemplo/%s","items":[]}"""
                .formatted(slug, PRECO, PRECO, transactionNsu, orderNsu, transactionNsu);

        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/webhooks/payment/" + InfinityPayGateway.PROVIDER)
                .header("x-infinitypay-signature", infinityPay.assinar(bytes))
                .contentType("application/json").content(bytes));
    }
}
