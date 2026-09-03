package com.betobanco.privacy.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Texto legal versionado. Documento Mestre V4.0, secao 22.
 *
 * <p>"Politica de privacidade e termos de uso versionados, com aceite
 * registrado (data, hora, IP, versao)." Uma versao nunca e editada: mudou o
 * texto, nasce outra linha. Editar em cima invalidaria todo aceite ja gravado,
 * porque o registro passaria a apontar para algo que a pessoa nao leu.
 */
@Entity
@Table(name = "legal_documents")
public class LegalDocument {

    public static final String TERMS_OF_USE = "TERMS_OF_USE";
    public static final String PRIVACY_POLICY = "PRIVACY_POLICY";
    public static final String COOKIE_POLICY = "COOKIE_POLICY";

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Secao 27 -- cada tenant tem os proprios termos: a razao social do contrato
     * e outra. Preenchido desde ja, mesmo com um unico tenant, porque acrescentar
     * a coluna depois obrigaria a revisar todo aceite ja gravado.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.RAIZ;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String body;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected LegalDocument() {
    }

    public LegalDocument(String type, String version, String body) {
        this.type = type;
        this.version = version;
        this.body = body;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getBody() {
        return body;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }
}
