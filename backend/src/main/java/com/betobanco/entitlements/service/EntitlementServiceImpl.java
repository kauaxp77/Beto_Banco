package com.betobanco.entitlements.service;

import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.entitlements.entity.Entitlement;
import com.betobanco.entitlements.repository.EntitlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class EntitlementServiceImpl implements EntitlementService {

    private final EntitlementRepository repo;

    public EntitlementServiceImpl(EntitlementRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Concessao conceder(UUID userId, UUID productId, String source, String sourceRef) {
        // Caminho comum: ja existe uma concessao vigente.
        var existente = repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId);
        if (existente.isPresent()) {
            return new Concessao(existente.get().getId(), false);
        }

        // O indice unico parcial e quem realmente garante a unicidade — a
        // consulta acima e so um atalho para o caso comum. Na corrida, o
        // ON CONFLICT devolve 0 linhas em vez de lancar: uma violacao de
        // constraint abortaria a transacao inteira no Postgres, inclusive a
        // do processador de webhooks dentro da qual este metodo roda.
        UUID id = UUID.randomUUID();
        int inseridas = repo.inserirSeNaoHouverVigente(id, userId, productId, source, sourceRef);
        if (inseridas == 1) {
            return new Concessao(id, true);
        }
        return repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId)
                .map(e -> new Concessao(e.getId(), false))
                .orElseThrow(() -> new IllegalStateException(
                        "concessao perdeu a corrida mas nenhuma vigente foi encontrada"));
    }

    @Override
    @Transactional
    public Concessao conceder(UUID userId, UUID productId, String source, String sourceRef,
                              Instant expiresAt) {
        if (expiresAt == null) {
            return conceder(userId, productId, source, sourceRef);
        }
        var existente = repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId);
        if (existente.isPresent()) {
            return new Concessao(existente.get().getId(), false);
        }
        UUID id = UUID.randomUUID();
        int inseridas = repo.inserirSeNaoHouverVigenteComPrazo(id, userId, productId, source,
                sourceRef, expiresAt);
        if (inseridas == 1) {
            return new Concessao(id, true);
        }
        return repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId)
                .map(e -> new Concessao(e.getId(), false))
                .orElseThrow(() -> new IllegalStateException(
                        "concessao perdeu a corrida mas nenhuma vigente foi encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> usuariosComAcesso(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return repo.usuariosComAcessoVigente(productIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemConcedido> listarPorSourceRefPrefixo(String prefixo) {
        return repo.findBySourceRefStartingWithOrderByGrantedAtDesc(prefixo).stream()
                .map(e -> new ItemConcedido(e.getId(), e.getUserId(), e.getProductId(),
                        e.getGrantedAt(), e.getExpiresAt(), e.getRevokedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void revogar(UUID userId, UUID productId) {
        repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId).ifPresent(e -> {
            e.revogar();
            repo.saveAndFlush(e);
        });
    }

    @Override
    @Transactional
    public int revogarPorOrigem(String sourceRef) {
        List<Entitlement> encontrados = repo.findBySourceRef(sourceRef);
        int revogados = 0;
        for (Entitlement e : encontrados) {
            if (e.getRevokedAt() == null) {
                e.revogar();
                revogados++;
            }
        }
        repo.saveAll(encontrados);
        repo.flush();
        return revogados;
    }

    @Override
    @Transactional
    public boolean revogarPorId(UUID userId, UUID entitlementId) {
        return repo.findById(entitlementId)
                .filter(e -> e.getUserId().equals(userId) && e.getRevokedAt() == null)
                .map(e -> {
                    e.revogar();
                    repo.saveAndFlush(e);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarAtivos() {
        return repo.countByRevokedAtIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean temAcesso(UUID userId, UUID productId) {
        return repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId)
                .filter(Entitlement::estaVigente)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Item> listarDe(UUID userId) {
        return repo.findByUserIdAndRevokedAtIsNull(userId).stream()
                .filter(Entitlement::estaVigente)
                .map(e -> new Item(e.getId(), e.getProductId(), e.getSource(),
                        e.getGrantedAt(), e.getExpiresAt()))
                .toList();
    }
}
