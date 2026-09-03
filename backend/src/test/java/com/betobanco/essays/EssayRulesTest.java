package com.betobanco.essays;

import com.betobanco.essays.entity.Essay;
import com.betobanco.essays.entity.EssayQuota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documento Mestre V4.0, secao 14 — as duas regras que o documento justifica em
 * dinheiro e em prazo.
 */
class EssayRulesTest {

    private static final UUID ALUNO = UUID.randomUUID();

    @Nested
    @DisplayName("Cota — o que mantem a margem da secao 04 de pe")
    class Cota {

        @Test
        void consomeAteAcabarEEntaoRecusa() {
            EssayQuota cota = new EssayQuota(ALUNO, EssayQuota.competenciaAtual(), 4);

            for (int i = 1; i <= 4; i++) {
                assertThat(cota.temSaldo()).as("antes do uso %d", i).isTrue();
                cota.consumir();
            }

            assertThat(cota.temSaldo()).isFalse();
            assertThat(cota.restantes()).isZero();
            assertThatThrownBy(cota::consumir)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("esgotada");
        }

        @Test
        void cotaZeradaNaoDeixaEnviarNemAPrimeira() {
            // Aluno sem mentoria e sem compra avulsa: nao ha correcao de graca.
            EssayQuota semCota = new EssayQuota(ALUNO, EssayQuota.competenciaAtual(), 0);

            assertThat(semCota.temSaldo()).isFalse();
            assertThatThrownBy(semCota::consumir).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void compraAvulsaSomaNaCompetenciaCorrente() {
            EssayQuota cota = new EssayQuota(ALUNO, EssayQuota.competenciaAtual(), 1);
            cota.consumir();
            assertThat(cota.temSaldo()).isFalse();

            cota.acrescentar(1);

            assertThat(cota.temSaldo()).isTrue();
            assertThat(cota.restantes()).isEqualTo(1);
        }

        @Test
        void aCompetenciaEOPrimeiroDiaDoMes() {
            // A cota e mensal e nao acumula: acumular deixaria um aluno inativo
            // por seis meses despejar 24 redacoes de uma vez na fila.
            LocalDate competencia = EssayQuota.competenciaAtual();

            assertThat(competencia.getDayOfMonth()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Prazo de 7 dias corridos, visivel ao aluno")
    class Prazo {

        @Test
        void nasceComSeteDiasDeprazo() {
            Essay redacao = novaRedacao();

            assertThat(redacao.getDueAt())
                    .isBetween(Instant.now().plus(6, ChronoUnit.DAYS).plus(23, ChronoUnit.HOURS),
                               Instant.now().plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES));
        }

        @Test
        void diasRestantesEPositivoNoEnvio() {
            assertThat(novaRedacao().diasRestantes()).isBetween(6L, 7L);
        }

        @Test
        void redacaoNovaEntraNaFilaDeCorrecao() {
            Essay redacao = novaRedacao();

            assertThat(redacao.getStatus()).isEqualTo(Essay.SUBMITTED);
            assertThat(redacao.aguardandoCorrecao()).isTrue();
        }

        @Test
        void redacaoCorrigidaSaiDaFila() {
            Essay redacao = novaRedacao();
            redacao.marcarEmCorrecao();
            assertThat(redacao.aguardandoCorrecao()).isTrue();

            redacao.marcarCorrigida();

            assertThat(redacao.getStatus()).isEqualTo(Essay.CORRECTED);
            assertThat(redacao.aguardandoCorrecao()).isFalse();
        }
    }

    @Nested
    @DisplayName("Reescrita — passo 5 do fluxo")
    class Reescrita {

        @Test
        void herdaTemaEBancaDaOriginalEApontaParaEla() {
            Essay original = novaRedacao();
            original.marcarCorrigida();

            Essay reescrita = Essay.reescritaDe(original, "https://storage/redacao-v2.pdf");

            assertThat(reescrita.getPrompt()).isEqualTo(original.getPrompt());
            assertThat(reescrita.getBoard()).isEqualTo(original.getBoard());
            assertThat(reescrita.getFileUrl()).isEqualTo("https://storage/redacao-v2.pdf");
            assertThat(reescrita.getStatus()).isEqualTo(Essay.SUBMITTED);
        }

        @Test
        void aReescritaGanhaPrazoProprioDeSeteDias() {
            Essay reescrita = Essay.reescritaDe(novaRedacao(), "https://storage/v2.pdf");

            assertThat(reescrita.diasRestantes()).isBetween(6L, 7L);
        }
    }

    private static Essay novaRedacao() {
        return new Essay(ALUNO, "Os desafios da inclusão financeira no Brasil",
                "CESGRANRIO", "https://storage/redacao.pdf");
    }
}
