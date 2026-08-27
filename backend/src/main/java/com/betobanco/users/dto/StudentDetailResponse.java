package com.betobanco.users.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDetailResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        List<String> roles,
        Instant createdAt) {
}
