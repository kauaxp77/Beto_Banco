package com.betobanco.invites.dto;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
        UUID entitlementId,
        String email,
        String fullName,
        UUID productId,
        String productName,
        Instant grantedAt,
        Instant expiresAt,
        boolean revoked,
        boolean contaNova) {
}
