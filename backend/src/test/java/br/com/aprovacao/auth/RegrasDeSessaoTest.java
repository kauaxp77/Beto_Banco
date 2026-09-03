package br.com.aprovacao.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Secao 20 -- regras de sessao, perfis e 2FA. Secao 21 -- bloqueio por tentativa. */
class RegrasDeSessaoTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Nested
    @DisplayName("Secao 21 -- 5 falhas levam a 15 min de bloqueio")
    class BloqueioDeLogin {

        @Test
        void bloqueiaExatamenteNaQuintaFalha() {
            Usuario usuario = novoUsuario();

            for (int tentativa = 1; tentativa <= 4; tentativa++) {
                assertThat(usuario.registrarFalhaDeLogin(5, 15))
                        .as("tentativa %d nao deve bloquear", tentativa)
                        .isFalse();
                assertThat(usuario.estaBloqueado()).isFalse();
            }
            assertThat(usuario.registrarFalhaDeLogin(5, 15)).isTrue();
            assertThat(usuario.estaBloqueado()).isTrue();
        }

        @Test
        void loginBemSucedidoZeraOContadorEDesbloqueia() {
            Usuario usuario = novoUsuario();
            usuario.registrarFalhaDeLogin(5, 15);
            usuario.registrarFalhaDeLogin(5, 15);

            usuario.registrarLoginBemSucedido();

            assertThat(usuario.estaBloqueado()).isFalse();
            assertThat(usuario.getUltimoAcessoEm()).isNotNull();
            // O contador voltou a zero: sao mais cinco falhas ate bloquear de novo.
            for (int i = 0; i < 4; i++) {
                assertThat(usuario.registrarFalhaDeLogin(5, 15)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Secao 20 -- perfis e 2FA obrigatorio para Admin e Suporte")
    class Perfis {

        @Test
        void novoUsuarioNasceApenasComoAluno() {
            assertThat(novoUsuario().getPerfis()).containsExactly(Perfil.ALUNO);
        }

        @Test
        void adminExigeSegundoFator() {
            Usuario usuario = novoUsuario();
            usuario.concederPerfil(Perfil.ADMIN);

            assertThat(usuario.exigeMfa(java.util.Set.of("ADMIN", "SUPORTE"))).isTrue();
        }

        @Test
        void alunoNaoExigeSegundoFator() {
            assertThat(novoUsuario().exigeMfa(java.util.Set.of("ADMIN", "SUPORTE"))).isFalse();
        }

        @Test
        void aOrdemDoEnumCasaComOsIdsDaTabelaPerfil() {
            // A ElementCollection grava perfil_id via ConversorPerfil (ordinal + 1).
            // Reordenar o enum reescreveria o significado de toda linha ja gravada.
            ConversorPerfil conversor = new ConversorPerfil();
            assertThat(conversor.convertToDatabaseColumn(Perfil.ALUNO)).isEqualTo((short) 1);
            assertThat(conversor.convertToDatabaseColumn(Perfil.SUPER_ADMIN)).isEqualTo((short) 6);
            assertThat(conversor.convertToEntityAttribute((short) 5)).isEqualTo(Perfil.ADMIN);
        }
    }

    @Nested
    @DisplayName("Secao 22 -- exclusao anonimiza e preserva o registro fiscal")
    class Anonimizacao {

        @Test
        void removeDadoPessoalMasMantemOIdentificador() {
            Usuario usuario = novoUsuario();
            usuario.setCpf("123.456.789-00");
            usuario.setWhatsapp("11999999999");
            UUID idOriginal = usuario.getId();

            usuario.anonimizar();

            assertThat(usuario.getId()).isEqualTo(idOriginal);
            assertThat(usuario.getCpf()).isNull();
            assertThat(usuario.getWhatsapp()).isNull();
            assertThat(usuario.getNome()).isEqualTo("Titular removido");
            assertThat(usuario.getEmail()).doesNotContain("aluno@exemplo.com");
            assertThat(usuario.getExcluidoEm()).isNotNull();
        }

        @Test
        void oEmailAnonimizadoContinuaUnicoPorConta() {
            Usuario um = novoUsuario();
            Usuario outro = novoUsuario();
            um.anonimizar();
            outro.anonimizar();

            assertThat(um.getEmail()).isNotEqualTo(outro.getEmail());
        }
    }

    @Nested
    @DisplayName("Secao 20 -- sessao e rotacao de refresh")
    class Sessoes {

        @Test
        void sessaoRevogadaDeixaDeEstarViva() {
            Sessao sessao = novaSessao();
            assertThat(sessao.estaViva()).isTrue();

            sessao.revogar("ROTACIONADO");

            assertThat(sessao.estaViva()).isFalse();
            assertThat(sessao.getMotivoRevogacao()).isEqualTo("ROTACIONADO");
        }

        @Test
        void revogarDuasVezesNaoSobrescreveOMotivoOriginal() {
            Sessao sessao = novaSessao();
            sessao.revogar("ROTACIONADO");
            sessao.revogar("REUSO_DE_REFRESH");

            // O primeiro motivo e o que explica o que aconteceu com a sessao.
            assertThat(sessao.getMotivoRevogacao()).isEqualTo("ROTACIONADO");
        }

        @Test
        void sessaoVencidaNaoEstaViva() {
            Sessao vencida = new Sessao(UUID.randomUUID(), UUID.randomUUID(), "hash",
                    "Chrome", "UA", "127.0.0.1", Instant.now().minusSeconds(1));
            assertThat(vencida.estaViva()).isFalse();
        }

        private Sessao novaSessao() {
            return new Sessao(UUID.randomUUID(), UUID.randomUUID(), "hash",
                    "Chrome", "UA", "127.0.0.1", Instant.now().plus(30, ChronoUnit.DAYS));
        }
    }

    @Nested
    @DisplayName("Secao 20 -- TOTP do segundo fator")
    class SegundoFator {

        @Test
        void recusaCodigoComTamanhoErrado() {
            String segredo = ServicoTotp.gerarSegredo();
            assertThat(ServicoTotp.codigoValido(segredo, "12345")).isFalse();
            assertThat(ServicoTotp.codigoValido(segredo, "1234567")).isFalse();
        }

        @Test
        void recusaSegredoOuCodigoAusente() {
            assertThat(ServicoTotp.codigoValido(null, "123456")).isFalse();
            assertThat(ServicoTotp.codigoValido(ServicoTotp.gerarSegredo(), null)).isFalse();
        }

        @Test
        void doisSegredosGeradosNuncaSaoIguais() {
            assertThat(ServicoTotp.gerarSegredo()).isNotEqualTo(ServicoTotp.gerarSegredo());
        }
    }

    private static Usuario novoUsuario() {
        return new Usuario(TENANT, "Aluno", "aluno@exemplo.com", "$2a$12$hashfalso");
    }
}
