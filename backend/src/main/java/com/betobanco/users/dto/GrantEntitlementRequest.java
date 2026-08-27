package com.betobanco.users.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantEntitlementRequest(@NotNull UUID productId) {
}
