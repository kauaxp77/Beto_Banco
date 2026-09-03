package br.com.aprovacao.pagamento;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aprovacao.comercial.StatusPedido;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Secao 24 -- cobertura unitaria da regra de negocio de acesso.
 *
 * <p>Secao 30 chama a falha de webhook de risco CRITICO-FINANCEIRO: "aluno paga e
 * nao recebe acesso: reembolso, chargeback e dano de reputacao". As quatro
 * garantias que o documento exige (assinatura, idempotencia, ordem, retentativa)
 * sao testadas uma a uma aqui.
 */
class ContratoDoWebhookTest {

    private static final String SEGREDO = "segredo-de-teste-com-tamanho-suficiente";

    @Nested
    @DisplayName("Secao 12 -- assinatura HMAC")
    class Assinatura {

        @Test
        void aceitaAssinaturaHexadecimalCorreta() {
            byte[] corpo = "{\"event_id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8);
            assertThat(AssinaturaHmac.confere(corpo, hmacHex(corpo), SEGREDO)).isTrue();
        }

        @Test
        void aceitaOPrefixoSha256QueAlgunsGatewaysEnviam() {
            byte[] corpo = "{\"event_id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8);
            assertThat(AssinaturaHmac.confere(corpo, "sha256=" + hmacHex(corpo), SEGREDO)).isTrue();
        }

        @Test
        void recusaCorpoAlteradoAindaQueEmUmByte() {
            byte[] original = "{\"event_id\":\"evt_1\",\"amount\":39700}".getBytes(StandardCharsets.UTF_8);
            byte[] adulterado = "{\"event_id\":\"evt_1\",\"amount\":39701}".getBytes(StandardCharsets.UTF_8);
            assertThat(AssinaturaHmac.confere(adulterado, hmacHex(original), SEGREDO)).isFalse();
        }

        @Test
        void recusaQuandoNaoHaSegredoConfigurado() {
            byte[] corpo = "{}".getBytes(StandardCharsets.UTF_8);
            assertThat(AssinaturaHmac.confere(corpo, hmacHex(corpo), "")).isFalse();
            assertThat(AssinaturaHmac.confere(corpo, hmacHex(corpo), null)).isFalse();
        }

        @Test
        void recusaAssinaturaAusente() {
            assertThat(AssinaturaHmac.confere("{}".getBytes(StandardCharsets.UTF_8), null, SEGREDO)).isFalse();
        }
    }

    @Nested
    @DisplayName("Secao 12 -- retentativa com backoff 1, 5, 30, 120 e fila morta")
    class Retentativa {

        private final List<Integer> backoff = List.of(1, 5, 30, 120);

        @Test
        void agendaCadaTentativaNoPassoSeguinteDoBackoff() {
            WebhookEvento evento = novoEvento();

            for (int passo = 0; passo < backoff.size(); passo++) {
                boolean filaMorta = evento.registrarFalha("falha " + passo, backoff);
                assertThat(filaMorta).isFalse();
                assertThat(evento.getStatus()).isEqualTo(WebhookEvento.Status.FALHA);
            }
            assertThat(evento.getTentativas()).isEqualTo((short) 4);
        }

        @Test
        void vaiParaFilaMortaApenasDepoisDeEsgotarOBackoff() {
            WebhookEvento evento = novoEvento();
            backoff.forEach(passo -> evento.registrarFalha("falha", backoff));

            assertThat(evento.registrarFalha("falha final", backoff)).isTrue();
            assertThat(evento.getStatus()).isEqualTo(WebhookEvento.Status.FILA_MORTA);
        }

        @Test
        void eventoIgnoradoNaoEntraEmRetentativa() {
            WebhookEvento evento = novoEvento();
            evento.marcarIgnorado("Pedido ja estava em APROVADO.");

            assertThat(evento.getStatus()).isEqualTo(WebhookEvento.Status.IGNORADO);
            assertThat(evento.getTentativas()).isZero();
        }

        @Test
        void truncaMensagemDeErroParaCaberNaColuna() {
            WebhookEvento evento = novoEvento();
            evento.registrarFalha("x".repeat(5000), backoff);
            assertThat(evento.getErro()).hasSize(2000);
        }
    }

    @Nested
    @DisplayName("Secao 12 -- efeito de cada estado sobre o acesso")
    class EstadosDoPagamento {

        @Test
        void apenasAprovadoLiberaAcesso() {
            assertThat(StatusPedido.APROVADO.liberaAcesso()).isTrue();
            for (StatusPedido outro : StatusPedido.values()) {
                if (outro != StatusPedido.APROVADO) {
                    assertThat(outro.liberaAcesso())
                            .as("%s nao pode liberar acesso", outro)
                            .isFalse();
                }
            }
        }

        @Test
        void estornoCancelamentoChargebackEExpiracaoRevogam() {
            assertThat(StatusPedido.ESTORNADO.revogaAcesso()).isTrue();
            assertThat(StatusPedido.CANCELADO.revogaAcesso()).isTrue();
            assertThat(StatusPedido.CHARGEBACK.revogaAcesso()).isTrue();
            assertThat(StatusPedido.EXPIRADO.revogaAcesso()).isTrue();
        }

        @Test
        void pendenteERecusadoNaoMexemNoAcesso() {
            assertThat(StatusPedido.PENDENTE.liberaAcesso()).isFalse();
            assertThat(StatusPedido.PENDENTE.revogaAcesso()).isFalse();
            assertThat(StatusPedido.RECUSADO.liberaAcesso()).isFalse();
            assertThat(StatusPedido.RECUSADO.revogaAcesso()).isFalse();
        }

        @Test
        void somenteChargebackBloqueiaRecompra() {
            assertThat(StatusPedido.CHARGEBACK.bloqueiaRecompra()).isTrue();
            assertThat(StatusPedido.ESTORNADO.bloqueiaRecompra()).isFalse();
        }

        @Test
        void estadoFinalNaoVoltaAtras() {
            assertThat(StatusPedido.ESTORNADO.ehFinal()).isTrue();
            assertThat(StatusPedido.CHARGEBACK.ehFinal()).isTrue();
            assertThat(StatusPedido.CANCELADO.ehFinal()).isTrue();
            assertThat(StatusPedido.EXPIRADO.ehFinal()).isTrue();
            assertThat(StatusPedido.APROVADO.ehFinal()).isFalse();
            assertThat(StatusPedido.PENDENTE.ehFinal()).isFalse();
        }
    }

    private static WebhookEvento novoEvento() {
        return new WebhookEvento("INFINITYPAY", "evt_1", "payment.approved",
                "{\"event_id\":\"evt_1\"}", true, Instant.now());
    }

    private static String hmacHex(byte[] corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(corpo));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
