package com.betobanco.entitlements.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrato que o modulo {@code entitlements} publica.
 *
 * <p>E aqui que mora a resposta para "esta pessoa tem acesso a isto?".
 * {@code payments} concede; {@code catalog} e os sub-projetos de conteudo
 * consultam. Nenhum deles conhece a tabela.
 */
public interface EntitlementService {

    /**
     * Concede acesso de forma idempotente: conceder duas vezes o mesmo produto
     * ao mesmo aluno devolve o entitlement que ja existia, sem duplicar.
     */
    Concessao conceder(UUID userId, UUID productId, String source, String sourceRef);

    void revogar(UUID userId, UUID productId);

    /** Revoga tudo que foi concedido por um pagamento — usado em estorno. */
    int revogarPorOrigem(String sourceRef);

    /**
     * Revoga um entitlement especifico, usado pela gestao do admin. O
     * {@code userId} e conferido de proposito: um id de entitlement de outro
     * aluno na URL nao pode revogar nada.
     *
     * @return true se revogou; false se nao existe, nao e deste aluno ou ja
     *         estava revogado.
     */
    boolean revogarPorId(UUID userId, UUID entitlementId);

    boolean temAcesso(UUID userId, UUID productId);

    List<Item> listarDe(UUID userId);

    record Concessao(UUID entitlementId, boolean criadoAgora) {
    }

    record Item(UUID entitlementId, UUID productId, String source, Instant grantedAt,
                Instant expiresAt) {
    }
}
