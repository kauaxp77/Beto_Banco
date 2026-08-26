package com.betobanco.users.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato que o modulo {@code users} publica para os demais.
 *
 * <p>E o unico caminho por onde outro modulo alcanca identidade: as entidades
 * e os repositorios de {@code users} sao consumo interno, e o teste ArchUnit
 * {@code nenhumModuloAcessaEntityOuRepositoryDeOutro} reprova o build se
 * alguem cruzar essa linha.
 */
public interface UserDirectory {

    /** Devolve o usuario apenas se ele existir e estiver ativo. */
    Optional<UserAccount> buscarAtivoPorId(UUID id);

    Optional<UserAccount> buscarPorEmail(String email);

    boolean existeEmail(String email);

    /**
     * Verifica a senha de um usuario ativo. Se o hash estiver num algoritmo
     * antigo, promove-o para o atual antes de devolver — a migracao acontece
     * aqui dentro, porque so este modulo conhece o formato do hash.
     */
    Optional<UserAccount> verificarCredenciais(String email, String senha);

    /** Cadastro publico: cria a conta como aluno, sem nenhum entitlement. */
    UserAccount registrar(String email, String senha, String nomeCompleto);

    void redefinirSenha(UUID userId, String novaSenha);
}
