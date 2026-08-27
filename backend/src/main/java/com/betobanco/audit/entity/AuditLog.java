package com.betobanco.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private String result = "SUCCESS";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID actorUserId, String action, String entityType, String entityId,
                    String metadata) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
