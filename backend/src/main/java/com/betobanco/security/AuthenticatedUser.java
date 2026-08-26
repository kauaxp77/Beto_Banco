package com.betobanco.security;

import java.util.Set;
import java.util.UUID;

/**
 * Identidade extraida do access token. E este o objeto injetado por
 * {@code @AuthenticationPrincipal} nos controllers — a identidade vem sempre
 * do token, nunca do corpo ou da URL da requisicao.
 */
public record AuthenticatedUser(UUID id, String email, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
