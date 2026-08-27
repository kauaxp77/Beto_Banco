package com.betobanco.users.dto;

import java.time.Instant;
import java.util.UUID;

public record StudentSummaryResponse(
        UUID id,
        String email,
        String fullName,
        String status,
        Instant createdAt) {
}
