package com.betobanco.contests.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Nivel 1 da arquitetura de conteudo. Documento Mestre V4.0, secao 07:
 * Carreira -> Orgao -> Cargo.
 *
 * <p>As carreiras de fase futura ficam cadastradas e inativas, e nao ausentes:
 * o escalonamento da secao 07 e uma decisao de quando publicar, nao de quando
 * modelar, e cadastrar depois obrigaria a reclassificar concursos ja existentes.
 */
@Entity
@Table(name = "careers")
public class Career {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column
    private String description;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active = true;

    protected Career() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public int getPosition() {
        return position;
    }

    public boolean isActive() {
        return active;
    }
}
