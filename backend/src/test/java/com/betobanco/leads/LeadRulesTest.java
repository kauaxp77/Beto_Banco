package com.betobanco.leads;

import com.betobanco.leads.entity.Lead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documento Mestre Premium V3.0, secoes 11 e 8 — as regras que decidem se o
 * CRM serve para ligar para alguem.
 */
class LeadRulesTest {

    @Nested
    @DisplayName("Uma pessoa, um lead")
    class Identidade {

        @Test
        void emailEGravadoEmMinusculas() {
            // O indice unico e sobre lower(email); gravar cru criaria a mesma
            // pessoa duas vezes e a equipe ligaria duas vezes para ela.
            Lead lead = new Lead("Ana Silva", "  Ana@Exemplo.COM ", null);

            assertThat(lead.getEmail()).isEqualTo("ana@exemplo.com");
        }

        @Test
        void normalizarAceitaNulo() {
            assertThat(Lead.normalizar(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Segundo contato não apaga o que o primeiro deu")
    class SegundoContato {

        @Test
        void whatsappEmBrancoNaoApagaOExistente() {
            // Esse numero costuma ser o unico jeito de falar com a pessoa.
            Lead lead = new Lead("Ana", "ana@exemplo.com", "61999990000");

            lead.registrarContato("Ana Silva", null);

            assertThat(lead.getWhatsapp()).isEqualTo("61999990000");
            assertThat(lead.getName()).isEqualTo("Ana Silva");
        }

        @Test
        void nomeEmBrancoNaoApagaOExistente() {
            Lead lead = new Lead("Ana Silva", "ana@exemplo.com", null);

            lead.registrarContato("   ", "61999990000");

            assertThat(lead.getName()).isEqualTo("Ana Silva");
            assertThat(lead.getWhatsapp()).isEqualTo("61999990000");
        }

        @Test
        void registrarContatoAtualizaOUltimoContato() throws InterruptedException {
            Lead lead = new Lead("Ana", "ana@exemplo.com", null);
            var antes = lead.getLastSeenAt();
            Thread.sleep(2);

            lead.registrarContato("Ana", null);

            assertThat(lead.getLastSeenAt()).isAfter(antes);
            // O primeiro contato nao se move: e ele que diz ha quanto tempo a
            // pessoa acompanha a plataforma.
            assertThat(lead.getFirstSeenAt()).isBefore(lead.getLastSeenAt());
        }
    }

    @Nested
    @DisplayName("Funil")
    class Funil {

        @Test
        void leadNasceComoNovo() {
            assertThat(new Lead("Ana", "ana@exemplo.com", null).getStatus()).isEqualTo(Lead.NEW);
        }

        @Test
        void avancaPelasEtapas() {
            Lead lead = new Lead("Ana", "ana@exemplo.com", null);

            lead.mudarStatus(Lead.CONTACTED);
            lead.mudarStatus(Lead.NEGOTIATING);

            assertThat(lead.getStatus()).isEqualTo(Lead.NEGOTIATING);
        }

        @Test
        void leadGanhoNaoReabre() {
            // Um lead que voltou a negociar e uma negociacao nova. Reabrir a
            // antiga falsearia a taxa de conversao do periodo.
            Lead lead = new Lead("Ana", "ana@exemplo.com", null);
            lead.mudarStatus(Lead.WON);

            assertThatThrownBy(() -> lead.mudarStatus(Lead.NEGOTIATING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já fechado");
        }

        @Test
        void leadPerdidoNaoReabre() {
            Lead lead = new Lead("Ana", "ana@exemplo.com", null);
            lead.mudarStatus(Lead.LOST);

            assertThatThrownBy(() -> lead.mudarStatus(Lead.CONTACTED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
