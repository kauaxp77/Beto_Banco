package com.betobanco.entitlements.service;

import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.entitlements.entity.Entitlement;
import com.betobanco.entitlements.repository.EntitlementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

        try {
            Entitlement novo = repo.saveAndFlush(
                    new Entitlement(userId, productId, source, sourceRef));
            return new Concessao(novo.getId(), true);
        } catch (DataIntegrityViolationException e) {
            // Corrida: outra transacao concedeu entre a consulta e a insercao.
            // O indice unico parcial e quem realmente garante a unicidade — a
            // consulta acima e so um atalho para o caso comum.
            return repo.findByUserIdAndProductIdAndRevokedAtIsNull(userId, productId)
                    .map(e2 -> new Concessao(e2.getId(), false))
                    .orElseThrow(() -> e);
        }
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
