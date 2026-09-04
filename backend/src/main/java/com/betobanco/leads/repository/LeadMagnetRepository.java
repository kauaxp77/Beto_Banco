package com.betobanco.leads.repository;

import com.betobanco.leads.entity.LeadMagnet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadMagnetRepository extends JpaRepository<LeadMagnet, UUID> {

    /**
     * Busca para a entrega publica: so material ativo.
     *
     * <p>O filtro de ativo mora aqui, e nao na tela, porque e ele que impede a
     * entrega de um material com arquivo ainda nao publicado — o que queimaria
     * o contato no momento exato em que ele acabou de ser dado.
     */
    Optional<LeadMagnet> findByTenantIdAndSlugAndActiveTrue(UUID tenantId, String slug);

    List<LeadMagnet> findByTenantIdAndActiveTrueOrderByTitleAsc(UUID tenantId);

    List<LeadMagnet> findByTenantIdOrderByTitleAsc(UUID tenantId);
}
