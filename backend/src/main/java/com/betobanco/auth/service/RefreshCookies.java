package com.betobanco.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fabrica dos cookies de refresh (spec 6.2). HttpOnly: o valor nunca e
 * visivel a JavaScript — XSS no frontend nao rouba a sessao de longa duracao.
 * Path restrito a /api/v1/auth: o navegador so envia o cookie para os
 * endpoints de auth, nunca para o resto da API. SameSite=Lax pressupoe a API
 * em subdominio do mesmo dominio do frontend.
 */
@Component
public class RefreshCookies {

    public static final String NOME = "bb_refresh";

    private final boolean secure;
    private final String sameSite;
    private final Duration validade;

    public RefreshCookies(@Value("${betobanco.auth.cookie-secure:true}") boolean secure,
                          @Value("${betobanco.auth.cookie-same-site:Lax}") String sameSite,
                          @Value("${betobanco.auth.refresh-token-days:30}") long dias) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.validade = Duration.ofDays(dias);
    }

    public ResponseCookie emitir(String valor) {
        return builder(valor).maxAge(validade).build();
    }

    public ResponseCookie limpar() {
        return builder("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true).secure(secure).sameSite(sameSite).path("/api/v1/auth");
    }
}
