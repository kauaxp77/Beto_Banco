package com.betobanco.courses;

import com.betobanco.courses.dto.LessonCardResponse;
import com.betobanco.courses.entity.LessonPlayback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documento Mestre Premium V3.0, secao 5 — "Continue assistindo" e progresso.
 */
class EngajamentoRulesTest {

    @Nested
    @DisplayName("Marca de onde parou")
    class Posicao {

        @Test
        void gravaOSegundoInformado() {
            LessonPlayback marca = nova();

            marca.marcar(742);

            assertThat(marca.getPositionSeconds()).isEqualTo(742);
        }

        @Test
        void segundoNegativoERecusado() {
            // Player que manda posicao negativa esta com defeito; gravar zero
            // esconderia isso ate alguem reclamar que o "continuar" volta
            // sempre para o comeco.
            assertThatThrownBy(() -> nova().marcar(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negativa");
        }

        @Test
        void zeroEValido() {
            LessonPlayback marca = nova();
            marca.marcar(300);

            marca.marcar(0);

            assertThat(marca.getPositionSeconds()).isZero();
        }

        private static LessonPlayback nova() {
            return new LessonPlayback(UUID.randomUUID(), UUID.randomUUID());
        }
    }

    @Nested
    @DisplayName("Percentual assistido")
    class Percentual {

        @Test
        void metadeDoVideo() {
            assertThat(cartao(1200, 600).percentual()).isEqualTo(50);
        }

        @Test
        void semDuracaoConhecidaNaoInventaProgresso() {
            // Aula sem duration_seconds cadastrada: melhor 0 do que um numero
            // arbitrario numa barra que o aluno usa para se orientar.
            assertThat(cartao(null, 600).percentual()).isZero();
        }

        @Test
        void posicaoAlemDaDuracaoNaoPassaDeCem() {
            // Acontece de verdade: o player reporta alguns segundos a mais que
            // a duracao cadastrada no fim do video.
            assertThat(cartao(1200, 1250).percentual()).isEqualTo(100);
        }

        @Test
        void semPosicaoENada() {
            assertThat(cartao(1200, null).percentual()).isZero();
        }

        private static LessonCardResponse cartao(Integer duracao, Integer posicao) {
            return new LessonCardResponse(UUID.randomUUID(), "Aula 4", UUID.randomUUID(),
                    "Conhecimentos Bancários", duracao, posicao, Instant.now());
        }
    }
}
