package com.betobanco.audit.dto;

import com.betobanco.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String action,
        String entityType,
        String entityId,
        String metadata,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getActorUserId(), a.getAction(),
                a.getEntityType(), a.getEntityId(), a.getMetadata(), a.getCreatedAt());
    }
}
