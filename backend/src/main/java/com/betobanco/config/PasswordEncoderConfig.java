package com.betobanco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Senhas novas em Argon2id; senhas legadas do Supabase, em bcrypt, continuam
 * validas. O DelegatingPasswordEncoder escolhe o algoritmo pelo prefixo {id}
 * do hash — por isso a migration V3 grava "{bcrypt}$2a$10$..." e nao o hash cru.
 */
@Configuration
public class PasswordEncoderConfig {

    public static final String ID_ATUAL = "argon2";
    public static final String PREFIXO_ATUAL = "{" + ID_ATUAL + "}";

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = Map.of(
                ID_ATUAL, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                "bcrypt", new BCryptPasswordEncoder());

        DelegatingPasswordEncoder delegating =
                new DelegatingPasswordEncoder(ID_ATUAL, encoders);

        // Hash sem prefixo nao e adivinhado. Este encoder de fallback so
        // participa de matches() e sempre devolve false — recusar de forma
        // limpa e melhor do que supor um algoritmo e comparar contra a
        // suposicao errada. Ele nunca codifica nada.
        delegating.setDefaultPasswordEncoderForMatches(new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                throw new UnsupportedOperationException(
                        "encode sempre usa o algoritmo atual: " + ID_ATUAL);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        });

        return delegating;
    }
}
