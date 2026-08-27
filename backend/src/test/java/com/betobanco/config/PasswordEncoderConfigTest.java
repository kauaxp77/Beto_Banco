package com.betobanco.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void senhaNovaEhCodificadaEmArgon2() {
        String hash = encoder.encode("senha-forte-123");

        assertThat(hash).startsWith(PasswordEncoderConfig.PREFIXO_ATUAL);
        assertThat(encoder.matches("senha-forte-123", hash)).isTrue();
        assertThat(encoder.matches("outra-senha", hash)).isFalse();
    }

    @Test
    void hashLegadoDoSupabaseContinuaValido() {
        String legado = "{bcrypt}" + new BCryptPasswordEncoder().encode("senha-antiga");

        assertThat(encoder.matches("senha-antiga", legado)).isTrue();
        assertThat(encoder.matches("senha-errada", legado)).isFalse();
    }

    @Test
    void hashSemPrefixoEhRecusadoSemExplodir() {
        // Documenta por que a migration V3 grava "{bcrypt}$2a$10$...": sem o
        // prefixo o DelegatingPasswordEncoder nao sabe qual algoritmo usar.
        // O comportamento correto e recusar, nao lancar excecao.
        String semPrefixo = new BCryptPasswordEncoder().encode("senha-antiga");

        assertThat(encoder.matches("senha-antiga", semPrefixo)).isFalse();
    }

    @Test
    void duasCodificacoesDaMesmaSenhaDiferem() {
        assertThat(encoder.encode("igual")).isNotEqualTo(encoder.encode("igual"));
    }
}
