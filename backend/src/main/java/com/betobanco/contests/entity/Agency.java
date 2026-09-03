package com.betobanco.contests.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Nivel 2: orgao. Secao 07. */
@Entity
@Table(name = "agencies")
public class Agency {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String acronym;

    /** FEDERAL, STATE, MUNICIPAL, DISTRICT. */
    @Column(nullable = false)
    private String sphere;

    @Column
    private String state;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    protected Agency() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym;
    }

    public String getSphere() {
        return sphere;
    }

    public String getState() {
        return state;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
