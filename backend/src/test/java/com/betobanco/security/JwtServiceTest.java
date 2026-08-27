package com.betobanco.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-para-hs256";

    private final JwtService jwt = new JwtService(SEGREDO, 15);

    @Test
    void tokenValidoDevolveOUsuarioComSuasRoles() {
        UUID id = UUID.randomUUID();
        String token = jwt.gerar(id, "aluno@exemplo.com", Set.of("ROLE_STUDENT"));

        AuthenticatedUser usuario = jwt.validar(token).orElseThrow();

        assertThat(usuario.id()).isEqualTo(id);
        assertThat(usuario.email()).isEqualTo("aluno@exemplo.com");
        assertThat(usuario.roles()).containsExactly("ROLE_STUDENT");
        assertThat(usuario.hasRole("ROLE_STUDENT")).isTrue();
        assertThat(usuario.hasRole("ROLE_ADMIN")).isFalse();
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        String forjado = new JwtService("outro-segredo-totalmente-diferente-com-32b", 15)
                .gerar(UUID.randomUUID(), "invasor@exemplo.com", Set.of("ROLE_ADMIN"));

        assertThat(jwt.validar(forjado)).isEmpty();
    }

    @Test
    void tokenExpiradoEhRecusado() {
        String token = new JwtService(SEGREDO, -1)
                .gerar(UUID.randomUUID(), "aluno@exemplo.com", Set.of("ROLE_STUDENT"));

        assertThat(jwt.validar(token)).isEmpty();
    }

    @Test
    void lixoNaoDerrubaAValidacao() {
        assertThat(jwt.validar("isto-nao-e-um-jwt")).isEmpty();
        assertThat(jwt.validar("")).isEmpty();
        assertThat(jwt.validar(null)).isEmpty();
    }

    @Test
    void doisTokensDoMesmoUsuarioTemIdentificadoresDistintos() {
        UUID id = UUID.randomUUID();

        assertThat(jwt.gerar(id, "a@b.com", Set.of("ROLE_STUDENT")))
                .isNotEqualTo(jwt.gerar(id, "a@b.com", Set.of("ROLE_STUDENT")));
    }

    @Test
    void aDuracaoEhExpostaEmSegundos() {
        assertThat(jwt.duracaoSegundos()).isEqualTo(900L);
    }
}
