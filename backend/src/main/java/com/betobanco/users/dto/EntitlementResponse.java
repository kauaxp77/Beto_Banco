package com.betobanco.users.dto;

import java.time.Instant;
import java.util.UUID;

public record EntitlementResponse(
        UUID entitlementId,
        UUID productId,
        String sku,
        String productName,
        String source,
        Instant grantedAt,
        Instant expiresAt) {
}
