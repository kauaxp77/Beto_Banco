package com.betobanco.contests;

import com.betobanco.contests.entity.Contest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documento Mestre V4.0, secao 11 — as regras que existem para proteger a
 * confianca do aluno no dado da ficha.
 */
class ContestRulesTest {

    private static final String FONTE = "https://www.bb.com.br/concursos";

    @Nested
    @DisplayName("Publicar exige ter conferido e ter contra o que conferir")
    class Publicacao {

        @Test
        void fichaNuncaVerificadaNaoPublica() {
            Contest ficha = nova();

            assertThatThrownBy(ficha::publicar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("seção 11");
            assertThat(ficha.publicado()).isFalse();
        }

        @Test
        void fichaVerificadaPublica() {
            Contest ficha = nova();
            ficha.registrarVerificacao(UUID.randomUUID(), FONTE);

            ficha.publicar();

            assertThat(ficha.publicado()).isTrue();
            assertThat(ficha.getSourceUrl()).isEqualTo(FONTE);
        }

        @Test
        void verificacaoSemFonteNaoEVerificacao() {
            // "Verificado" sem dizer contra o que nao prova nada, e a secao 11
            // pede o link justamente para o aluno poder conferir sozinho.
            Contest ficha = nova();

            assertThatThrownBy(() -> ficha.registrarVerificacao(UUID.randomUUID(), "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fonte oficial");
        }

        @Test
        void despublicarTiraDoArSemApagarAVerificacao() {
            Contest ficha = nova();
            ficha.registrarVerificacao(UUID.randomUUID(), FONTE);
            ficha.publicar();

            ficha.despublicar();

            assertThat(ficha.publicado()).isFalse();
            assertThat(ficha.getVerifiedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fila de revisão de 60 dias")
    class Revisao {

        @Test
        void fichaNuncaVerificadaEstaDefasada() {
            // O caso mais urgente da fila, nao o menos: nunca foi conferida.
            Contest ficha = nova();

            assertThat(ficha.verificacaoDefasada()).isTrue();
            assertThat(ficha.diasDesdeVerificacao()).isEqualTo(-1);
        }

        @Test
        void fichaRecemVerificadaNaoEstaDefasada() {
            Contest ficha = nova();
            ficha.registrarVerificacao(UUID.randomUUID(), FONTE);

            assertThat(ficha.verificacaoDefasada()).isFalse();
            assertThat(ficha.diasDesdeVerificacao()).isZero();
        }

        @Test
        void oPrazoDaVerificacaoEDeSessentaDias() {
            assertThat(Contest.VALIDADE_DA_VERIFICACAO.toDays()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("Inscrições abertas pela data, não pelo status")
    class Inscricoes {

        @Test
        void abertaQuandoHojeEstaDentroDoPeriodo() {
            Contest ficha = nova();
            ficha.setRegistrationStart(LocalDate.now().minusDays(3));
            ficha.setRegistrationEnd(LocalDate.now().plusDays(3));

            assertThat(ficha.inscricoesAbertas()).isTrue();
        }

        @Test
        void fechadaDepoisDoPrazoAindaQueOStatusNaoTenhaSidoAtualizado() {
            // O status e um campo que alguem precisa lembrar de mudar; a data
            // nao depende de ninguem. Quem responde ao aluno e a data.
            Contest ficha = nova();
            ficha.setStatus(Contest.REGISTRATION_OPEN);
            ficha.setRegistrationStart(LocalDate.now().minusDays(30));
            ficha.setRegistrationEnd(LocalDate.now().minusDays(1));

            assertThat(ficha.getStatus()).isEqualTo(Contest.REGISTRATION_OPEN);
            assertThat(ficha.inscricoesAbertas()).isFalse();
        }

        @Test
        void semDatasNaoAfirmaQueEstaAberta() {
            assertThat(nova().inscricoesAbertas()).isFalse();
        }
    }

    private static Contest nova() {
        return new Contest(UUID.randomUUID(),
                "Banco do Brasil — Escriturário 2027", "bb-escriturario-2027");
    }
}
