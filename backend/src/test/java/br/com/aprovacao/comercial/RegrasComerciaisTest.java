package br.com.aprovacao.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aprovacao.common.Dinheiro;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Secao 24 -- regra de negocio: cupom e calculo de valor (secoes 03 e 18). */
class RegrasComerciaisTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Nested
    @DisplayName("Secao 18 -- dinheiro em centavos, nunca float")
    class Valores {

        @ParameterizedTest(name = "{0} centavos com {1}% de desconto = {2}")
        @CsvSource({
                "39700, 10, 3970",   // Pix a vista, curso de R$ 397
                "69700, 30, 20910",  // teto de cupom sobre o combo de R$ 697
                "39700, 0,  0",
                "1,     30, 0",      // 0,3 centavo arredonda para baixo
                "5,     30, 2",      // 1,5 centavo arredonda meio-para-cima
        })
        void aplicaPercentualSemPerderCentavo(long centavos, int percentual, long esperado) {
            assertThat(Dinheiro.aplicarPercentual(centavos, percentual)).isEqualTo(esperado);
        }

        @Test
        void formataEmRealBrasileiro() {
            // O separador de milhar do java.text e um espaco nao quebravel; comparar
            // por digitos evita um teste que quebra a cada atualizacao do CLDR.
            assertThat(Dinheiro.formatar(39700).replaceAll("\\s", " "))
                    .contains("397,00")
                    .startsWith("R$");
        }
    }

    @Nested
    @DisplayName("Secao 03 -- cupom: teto de 30% e validade obrigatoria")
    class Cupons {

        @Test
        void recusaPercentualAcimaDoTetoDe30() {
            assertThatThrownBy(() -> novoCupom((short) 31, 1, daquiADias(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("teto de 30%");
        }

        @Test
        void recusaPercentualZeroOuNegativo() {
            assertThatThrownBy(() -> novoCupom((short) 0, 1, daquiADias(30)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void aceitaExatamenteOTeto() {
            assertThat(novoCupom((short) 30, 1, daquiADias(30)).getPercentual()).isEqualTo((short) 30);
        }

        @Test
        void cupomVencidoNaoEUtilizavel() {
            assertThat(novoCupom((short) 10, 100, Instant.now().minusSeconds(1)).utilizavel()).isFalse();
        }

        @Test
        void cupomEsgotadoNaoEUtilizavel() {
            Cupom cupom = novoCupom((short) 10, 2, daquiADias(30));
            cupom.consumir();
            cupom.consumir();

            assertThat(cupom.utilizavel()).isFalse();
            assertThatThrownBy(cupom::consumir).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void normalizaOCodigoParaMaiusculas() {
            assertThat(novoCupom("primeira-compra").getCodigo()).isEqualTo("PRIMEIRA-COMPRA");
        }

        private Cupom novoCupom(short percentual, int usosMax, Instant validoAte) {
            return new Cupom(TENANT, "PROMO", percentual, usosMax, validoAte);
        }

        private Cupom novoCupom(String codigo) {
            return new Cupom(TENANT, codigo, (short) 10, 1, daquiADias(30));
        }

        private Instant daquiADias(int dias) {
            return Instant.now().plus(dias, ChronoUnit.DAYS);
        }
    }

    @Nested
    @DisplayName("Secao 12 -- ciclo de vida do pedido")
    class CicloDoPedido {

        @Test
        void nascePendenteComExpiracaoDe72Horas() {
            Pedido pedido = novoPedido();

            assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PENDENTE);
            assertThat(pedido.getExpiraEm())
                    .isBetween(Instant.now().plus(71, ChronoUnit.HOURS),
                               Instant.now().plus(73, ChronoUnit.HOURS));
        }

        @Test
        void mudarParaOMesmoStatusNaoContaComoMudanca() {
            Pedido pedido = novoPedido();

            assertThat(pedido.mudarStatus(StatusPedido.APROVADO)).isTrue();
            assertThat(pedido.mudarStatus(StatusPedido.APROVADO)).isFalse();
        }

        @Test
        void descontaDoValorBrutoSemAlterarOValorOriginal() {
            Pedido pedido = novoPedido();
            pedido.aplicarDesconto(UUID.randomUUID(), Dinheiro.aplicarPercentual(39700, 10));

            assertThat(pedido.getValorCentavos()).isEqualTo(39700);
            assertThat(pedido.getDescontoCentavos()).isEqualTo(3970);
            assertThat(pedido.valorLiquidoCentavos()).isEqualTo(35730);
        }

        @Test
        void pedidoPendenteVencidoEDetectadoComoExpirado() {
            // -1 hora, e nao 0: com janela zero a expiracao cai no mesmo tick do
            // relogio da criacao e o teste passaria a depender da resolucao do clock.
            Pedido vencido = new Pedido(TENANT, "aluno@exemplo.com", "Aluno", 39700, "chave", -1);
            assertThat(vencido.expirou()).isTrue();
        }

        @Test
        void pedidoAprovadoNaoExpiraMesmoDepoisDoPrazoDoCheckout() {
            // expirou() so vale para PENDENTE: quem revoga acesso de pedido pago e o
            // fim dos 12 meses da matricula, nao a janela de 72h do checkout.
            Pedido pago = new Pedido(TENANT, "aluno@exemplo.com", "Aluno", 39700, "chave", -1);
            pago.mudarStatus(StatusPedido.APROVADO);

            assertThat(pago.expirou()).isFalse();
        }

        private Pedido novoPedido() {
            return new Pedido(TENANT, "Aluno@Exemplo.com", "Aluno", 39700, "chave-idem", 72);
        }
    }

    @Test
    void emailDoPedidoEGuardadoEmMinusculasParaCasarComOCadastro() {
        Pedido pedido = new Pedido(TENANT, "Aluno@Exemplo.COM", "Aluno", 39700, "k", 72);
        assertThat(pedido.getEmail()).isEqualTo("aluno@exemplo.com");
    }
}
