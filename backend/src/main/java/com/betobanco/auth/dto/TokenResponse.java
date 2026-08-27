package com.betobanco.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * O refresh token NUNCA sai no JSON (spec 6.2): ele viaja apenas no cookie
 * HttpOnly. O campo existe aqui so como veiculo interno entre AuthService e o
 * controller, que o move para o cookie e serializa o resto.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(String accessToken, @JsonIgnore String refreshToken, long expiresIn,
                            String tokenType) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}
