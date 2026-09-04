package com.betobanco.leads.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Material que troca conteudo por contato. Documento Mestre Premium V3.0,
 * secao 11: "PDF, Mapa Mental, Cronograma, Questoes — objetivo: Lead".
 *
 * <p>O {@code fileUrl} nunca sai na listagem publica. Ele e a contrapartida do
 * cadastro: exibi-lo antes tornaria o formulario opcional, e a captacao inteira
 * deixaria de existir.
 */
@Entity
@Table(name = "lead_magnets")
public class LeadMagnet {

    public static final String PDF = "PDF";
    public static final String MAPA_MENTAL = "MAPA_MENTAL";
    public static final String CRONOGRAMA = "CRONOGRAMA";
    public static final String QUESTOES = "QUESTOES";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String kind;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private boolean active = true;

    protected LeadMagnet() {
    }

    public LeadMagnet(String slug, String title, String kind, String fileUrl) {
        this.slug = slug;
        this.title = title;
        this.kind = kind;
        this.fileUrl = fileUrl;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getKind() {
        return kind;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
