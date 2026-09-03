package br.com.aprovacao.auth;

/**
 * Secao 20 -- seis perfis, permissao acumulativa.
 *
 * <p>A ordem declarada casa com a coluna perfil.id do banco (ORDINAL na
 * ElementCollection de {@link Usuario}). Perfil novo entra no fim; reordenar
 * quebraria toda linha ja gravada em usuario_perfil.
 */
public enum Perfil {
    ALUNO,
    PROFESSOR,
    CORRETOR,
    SUPORTE,
    ADMIN,
    SUPER_ADMIN;

    /** Authority no formato que o Spring Security espera em hasRole(). */
    public String authority() {
        return "ROLE_" + name();
    }
}
