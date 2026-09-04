package com.betobanco.auth.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrato que o modulo {@code auth} publica sobre as sessoes vigentes.
 *
 * <p>Secao 10 limita a dois dispositivos simultaneos, e a tela do player mostra
 * ao aluno de onde a conta esta aberta — sem isso, o limite chega ate ele como
 * um logout sem explicacao.
 *
 * <p>Existe para que a tela nao precise do {@code RefreshTokenRepository}: o
 * token de renovacao e credencial, e um modulo que alcanca o repositorio dele
 * alcanca tambem o hash, a revogacao e a rotacao. Aqui sai apenas o que a tela
 * exibe. O teste {@code nenhumModuloAcessaEntityOuRepositoryDeOutro} reprova o
 * build de quem cruzar essa linha.
 */
public interface ActiveSessions {

    /**
     * Sessoes ainda validas da conta, da mais antiga para a mais recente — a
     * mesma ordem em que o limite de dispositivos derruba, entao a primeira da
     * lista e a proxima a cair.
     */
    List<SessaoAtiva> vigentesDe(UUID userId);

    /**
     * O que a tela mostra de uma sessao. Sem identificador: o aluno enxerga as
     * sessoes, e encerrar uma delas e outra operacao, com outro contrato.
     */
    record SessaoAtiva(String ip, String userAgent, Instant issuedAt, Instant expiresAt) {
    }
}
