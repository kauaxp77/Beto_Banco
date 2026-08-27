package com.betobanco.entitlements.api;

import java.time.Instant;
import java.util.Collection;
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

    /**
     * Como {@link #conceder(UUID, UUID, String, String)}, mas com prazo:
     * usado pelos convites de cortesia. {@code expiresAt} nulo e vitalicio.
     * Se ja existe concessao vigente, ela e devolvida sem alterar o prazo.
     */
    Concessao conceder(UUID userId, UUID productId, String source, String sourceRef,
                       Instant expiresAt);

    /**
     * Usuarios distintos com acesso vigente a qualquer um dos produtos —
     * e a lista de destinatarios de um anuncio de curso.
     */
    List<UUID> usuariosComAcesso(Collection<UUID> productIds);

    /** Concessoes cujo sourceRef comeca com o prefixo — lista de convites. */
    List<ItemConcedido> listarPorSourceRefPrefixo(String prefixo);

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

    /** Quantas concessoes vigentes existem — para o dashboard do admin. */
    long contarAtivos();

    List<Item> listarDe(UUID userId);

    record Concessao(UUID entitlementId, boolean criadoAgora) {
    }

    record Item(UUID entitlementId, UUID productId, String source, Instant grantedAt,
                Instant expiresAt) {
    }

    /** Como {@link Item}, mas com o dono — para listagens administrativas. */
    record ItemConcedido(UUID entitlementId, UUID userId, UUID productId, Instant grantedAt,
                         Instant expiresAt, Instant revokedAt) {
    }
}
