package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkProductRequest(@NotNull UUID productId) {
}
