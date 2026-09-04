package com.betobanco.payments.gateway;

import com.betobanco.payments.api.PaymentNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O corpo que a InfinitePay realmente envia no Checkout Integrado.
 *
 * <p>Ele nao tem {@code event} nem {@code event_id}. Uma versao anterior desta
 * classe exigia os dois e devolvia vazio sem eles — ou seja, descartava em
 * silencio todo webhook real do provedor. O sintoma em producao seria o pior
 * possivel: o aluno paga e nao recebe acesso.
 */
class InfinityPayWebhookRealTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-aqui";

    /** Exatamente o exemplo da documentacao do Checkout Integrado. */
    private static final String CORPO_DOCUMENTADO = """
            {"invoice_slug":"abc123","amount":1000,"paid_amount":1010,"installments":1,
             "capture_method":"credit_card","transaction_nsu":"4b1f7c2e-uuid",
             "order_nsu":"0f2b9d64-uuid-do-pedido",
             "receipt_url":"https://comprovante.com/123","items":[]}""";

    private final InfinityPayGateway gateway =
            new InfinityPayGateway(new ObjectMapper(), SEGREDO);

    @Test
    @DisplayName("O corpo real é aceito, mesmo sem campo de evento")
    void corpoSemEventoEAceito() {
        PaymentNotification n = interpretar(CORPO_DOCUMENTADO);

        assertThat(n.tipo()).isEqualTo(PaymentNotification.Tipo.APROVADO);
        assertThat(n.transactionId()).isEqualTo("4b1f7c2e-uuid");
        assertThat(n.orderNsu()).isEqualTo("0f2b9d64-uuid-do-pedido");
        assertThat(n.invoiceSlug()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("A identidade do evento sai da transação quando não há event_id")
    void identidadeVemDaTransacao() {
        // E o que a deduplicacao do webhook usa; sem isso, uma reentrega
        // criaria um segundo pagamento para a mesma compra.
        assertThat(interpretar(CORPO_DOCUMENTADO).eventId()).isEqualTo("4b1f7c2e-uuid");
    }

    @Test
    @DisplayName("amount vem em centavos e NÃO é multiplicado por cem")
    void amountJaEstaEmCentavos() {
        // A documentacao mostra 1500 para R$ 15,00. Tratar como reais faria uma
        // venda de R$ 10,00 ser registrada como R$ 1.000,00.
        assertThat(interpretar(CORPO_DOCUMENTADO).amountCents()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Valor decimal em texto continua sendo lido como reais")
    void valorDecimalEmTextoEConvertido() {
        // Compatibilidade com contas em formato antigo. BigDecimal, e nao
        // double: (long)(29.90 * 100) devolve 2989.
        PaymentNotification n = interpretar("""
                {"event_id":"evt-1","event":"payment.approved","transaction_id":"tx-1",
                 "amount":"29.90"}""");

        assertThat(n.amountCents()).isEqualTo(2990L);
    }

    @Test
    @DisplayName("Corpo sem transação e sem evento é descartado")
    void corpoInutilEDescartado() {
        // Aqui devolver vazio e o certo: nao ha o que processar, e inventar um
        // identificador criaria pagamento fantasma.
        Optional<PaymentNotification> nada = gateway.interpretar(
                "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(nada).isEmpty();
    }

    @Test
    @DisplayName("Evento explícito prevalece sobre a inferência")
    void eventoExplicitoPrevalece() {
        PaymentNotification n = interpretar("""
                {"event_id":"evt-9","event":"payment.refunded","transaction_nsu":"tx-9",
                 "order_nsu":"pedido-9","amount":1000}""");

        assertThat(n.tipo()).isEqualTo(PaymentNotification.Tipo.REEMBOLSADO);
    }

    private PaymentNotification interpretar(String corpo) {
        return gateway.interpretar(corpo.getBytes(StandardCharsets.UTF_8)).orElseThrow(
                () -> new AssertionError("O gateway descartou um corpo que deveria aceitar."));
    }
}
