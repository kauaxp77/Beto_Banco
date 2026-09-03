package com.betobanco.payments.gateway;

import com.betobanco.payments.api.PaymentNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documento Mestre V4.0, secao 12 — contrato do webhook do gateway oficial.
 *
 * <p>O que se testa aqui e a fronteira: assinatura sobre bytes crus e traducao
 * do formato do provedor. Idempotencia, retry e liberacao de acesso ja tem
 * cobertura propria no fluxo do WebhookProcessor.
 */
class InfinityPayGatewayTest {

    private static final String SEGREDO = "segredo-de-teste-da-infinitypay-com-tamanho-suficiente";

    private final ObjectMapper mapper = new ObjectMapper();
    private final InfinityPayGateway gateway = new InfinityPayGateway(mapper, SEGREDO);

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Nested
    class Assinatura {

        @Test
        void aceitaAssinaturaCorretaEmHexadecimal() {
            byte[] corpo = bytes("{\"event_id\":\"evt_1\",\"event\":\"paid\"}");

            assertThat(gateway.assinaturaValida(corpo, Map.of("x-signature", gateway.assinar(corpo))))
                    .isTrue();
        }

        @ParameterizedTest(name = "cabecalho {0}")
        @ValueSource(strings = {"x-infinitypay-signature", "x-signature", "signature"})
        void aceitaOsTresNomesDeCabecalhoQueOProvedorUsa(String cabecalho) {
            byte[] corpo = bytes("{\"event_id\":\"evt_1\",\"event\":\"paid\"}");

            assertThat(gateway.assinaturaValida(corpo, Map.of(cabecalho, gateway.assinar(corpo))))
                    .isTrue();
        }

        @Test
        void aceitaOPrefixoSha256EOFormatoBase64() {
            byte[] corpo = bytes("{\"event_id\":\"evt_1\",\"event\":\"paid\"}");
            String hex = gateway.assinar(corpo);
            String base64 = Base64.getEncoder()
                    .encodeToString(java.util.HexFormat.of().parseHex(hex));

            assertThat(gateway.assinaturaValida(corpo, Map.of("x-signature", "sha256=" + hex))).isTrue();
            assertThat(gateway.assinaturaValida(corpo, Map.of("x-signature", base64))).isTrue();
        }

        @Test
        void recusaCorpoAlteradoEmUmUnicoByte() {
            byte[] original = bytes("{\"event_id\":\"evt_1\",\"amount_cents\":39700}");
            byte[] adulterado = bytes("{\"event_id\":\"evt_1\",\"amount_cents\":39701}");
            String assinaturaDoOriginal = gateway.assinar(original);

            assertThat(gateway.assinaturaValida(adulterado, Map.of("x-signature", assinaturaDoOriginal)))
                    .isFalse();
        }

        @Test
        void recusaQuandoNaoHaCabecalhoDeAssinatura() {
            assertThat(gateway.assinaturaValida(bytes("{}"), Map.of())).isFalse();
        }

        @Test
        void recusaTudoQuandoOSegredoNaoFoiConfigurado() {
            // Deploy sem INFINITYPAY_WEBHOOK_SECRET nao pode virar "aceita
            // qualquer coisa": e assim que nasce acesso sem pagamento.
            InfinityPayGateway semSegredo = new InfinityPayGateway(mapper, "");
            byte[] corpo = bytes("{\"event_id\":\"evt_1\"}");

            assertThat(semSegredo.assinaturaValida(corpo, Map.of("x-signature", "qualquer")))
                    .isFalse();
        }
    }

    @Nested
    class Traducao {

        @Test
        void interpretaPayloadAninhadoEmData() {
            byte[] corpo = bytes("""
                    {
                      "event": "charge.paid",
                      "event_id": "evt_abc",
                      "occurred_at": "2026-09-03T10:15:30Z",
                      "data": {
                        "charge_id": "ch_123",
                        "reference": "CURSO-BB",
                        "customer_email": "aluno@exemplo.com",
                        "customer_name": "Aluno Teste",
                        "amount_cents": 39700,
                        "currency": "BRL"
                      }
                    }
                    """);

            PaymentNotification n = gateway.interpretar(corpo).orElseThrow();

            assertThat(n.eventId()).isEqualTo("evt_abc");
            assertThat(n.tipo()).isEqualTo(PaymentNotification.Tipo.APROVADO);
            assertThat(n.transactionId()).isEqualTo("ch_123");
            assertThat(n.sku()).isEqualTo("CURSO-BB");
            assertThat(n.buyerEmail()).isEqualTo("aluno@exemplo.com");
            assertThat(n.amountCents()).isEqualTo(39700L);
            assertThat(n.occurredAt()).isEqualTo(Instant.parse("2026-09-03T10:15:30Z"));
        }

        @Test
        void interpretaPayloadPlanoSemONoData() {
            byte[] corpo = bytes("""
                    {"event_id":"evt_1","status":"paid","transaction_id":"tx_9",
                     "sku":"CURSO-BB","buyer_email":"a@b.c","amount_cents":1000}
                    """);

            PaymentNotification n = gateway.interpretar(corpo).orElseThrow();

            assertThat(n.tipo()).isEqualTo(PaymentNotification.Tipo.APROVADO);
            assertThat(n.transactionId()).isEqualTo("tx_9");
        }

        @Test
        void converteValorDecimalSemPerderCentavo() {
            // Secao 18: dinheiro em centavos, inteiro, nunca float.
            // (long)(29.90 * 100) devolveria 2989 em ponto flutuante.
            byte[] corpo = bytes("{\"event_id\":\"e\",\"event\":\"paid\",\"amount\":\"29.90\"}");

            assertThat(gateway.interpretar(corpo).orElseThrow().amountCents()).isEqualTo(2990L);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "paid,                  APROVADO",
                "charge.paid,           APROVADO",
                "transaction.approved,  APROVADO",
                "pending,               PENDENTE",
                "charge.canceled,       CANCELADO",
                "payment.refunded,      REEMBOLSADO",
                "chargeback,            CHARGEBACK",
        })
        void traduzOVocabularioDoProvedor(String evento, PaymentNotification.Tipo esperado) {
            byte[] corpo = bytes("{\"event_id\":\"e\",\"event\":\"" + evento + "\"}");

            assertThat(gateway.interpretar(corpo).orElseThrow().tipo()).isEqualTo(esperado);
        }

        @Test
        void statusDesconhecidoViraIgnoradoENuncaAprovado() {
            byte[] corpo = bytes("{\"event_id\":\"e\",\"event\":\"payment.some_new_status\"}");

            assertThat(gateway.interpretar(corpo).orElseThrow().tipo())
                    .isEqualTo(PaymentNotification.Tipo.IGNORADO);
        }

        @Test
        void semDataDeclaradaOMomentoFicaNuloEAFilaTrataPrimeiro() {
            byte[] corpo = bytes("{\"event_id\":\"e\",\"event\":\"paid\"}");

            assertThat(gateway.interpretar(corpo).orElseThrow().occurredAt()).isNull();
        }

        @Test
        void aceitaMomentoEmEpochDeSegundos() {
            byte[] corpo = bytes("{\"event_id\":\"e\",\"event\":\"paid\",\"timestamp\":\"1788000000\"}");

            assertThat(gateway.interpretar(corpo).orElseThrow().occurredAt())
                    .isEqualTo(Instant.ofEpochSecond(1788000000L));
        }

        @Test
        void payloadSemEventIdNaoEInterpretavel() {
            assertThat(gateway.interpretar(bytes("{\"event\":\"paid\"}"))).isEmpty();
        }

        @Test
        void corpoQueNaoEJsonNaoDerrubaOGateway() {
            assertThat(gateway.interpretar(bytes("isto nao e json"))).isEmpty();
        }
    }

    @Test
    void oProviderEOqueVaiGravadoEmPaymentsProvider() {
        assertThat(gateway.provider()).isEqualTo("infinitypay");
    }
}
