package com.betobanco.shared.tenant;

import java.util.UUID;

/**
 * Tenant da requisicao corrente. Documento Mestre V4.0, secao 27.
 *
 * <p>Hoje a plataforma tem um unico tenant e o isolamento no banco esta em modo
 * permissivo (ver V13). Esta classe existe agora, e nao na Fase 5, porque e o
 * ponto que todo codigo novo deve consultar em vez de assumir "o unico tenant":
 * quando o segundo cliente chegar, a mudanca e passar a preencher o contexto,
 * nao caçar cada consulta escrita nos meses anteriores.
 *
 * <p>Secao 27, consequencia: "Se tenant_id nao entrar na Fase 1, a Fase 5 vira
 * reescrita do backend inteiro."
 */
public final class TenantContext {

    /**
     * Tenant raiz, com id fixo na V13. Constante para que a aplicacao nao gaste
     * uma consulta por requisicao so para descobrir o unico tenant que existe.
     */
    public static final UUID RAIZ = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final ThreadLocal<UUID> ATUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Tenant corrente; o raiz enquanto nenhum filtro definir outro. */
    public static UUID atual() {
        UUID t = ATUAL.get();
        return t != null ? t : RAIZ;
    }

    public static void definir(UUID tenantId) {
        ATUAL.set(tenantId);
    }

    /**
     * Obrigatorio no fim de toda requisicao. Sem o limpar, a thread devolvida ao
     * pool leva o tenant para a requisicao seguinte -- vazamento entre clientes,
     * nao apenas um dado errado na tela.
     */
    public static void limpar() {
        ATUAL.remove();
    }
}
