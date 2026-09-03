package com.betobanco.privacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Pedido do titular sobre os proprios dados. Secao 22, portal do titular.
 *
 * <p>Gravado mesmo quando o atendimento e imediato — exportar e revogar
 * acontecem na hora. O registro existe porque o encarregado (DPO) precisa
 * conseguir demonstrar o que foi pedido, quando, e em quanto tempo foi
 * atendido; sem ele nao ha como responder a ANPD com evidencia.
 *
 * <p>O e-mail e copiado em coluna propria de proposito: no pedido de exclusao,
 * a conta e anonimizada logo em seguida e o vinculo com {@code users} perde o
 * dado. Guardar aqui mantem o pedido rastreavel sem ressuscitar o cadastro.
 */
@Entity
@Table(name = "data_subject_requests")
public class DataSubjectRequest {

    public static final String EXPORT = "EXPORT";
    public static final String RECTIFICATION = "RECTIFICATION";
    public static final String CONSENT_WITHDRAWAL = "CONSENT_WITHDRAWAL";
    public static final String DELETION = "DELETION";

    public static final String COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status = COMPLETED;

    @Column
    private String detail;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected DataSubjectRequest() {
    }

    public DataSubjectRequest(UUID userId, String userEmail, String type, String detail) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.type = type;
        this.detail = detail;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
