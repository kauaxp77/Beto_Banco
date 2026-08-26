package com.betobanco.users.api;

import java.util.Set;
import java.util.UUID;

/**
 * Visao publica de um usuario, para consumo dos outros modulos.
 *
 * <p>Nao carrega o hash da senha de proposito: nenhum modulo fora de
 * {@code users} precisa dele, e o que nao trafega nao vaza. Quem precisa
 * verificar uma senha chama {@link UserDirectory#verificarCredenciais}.
 */
public record UserAccount(UUID id, String email, String fullName, Set<String> roles) {
}
