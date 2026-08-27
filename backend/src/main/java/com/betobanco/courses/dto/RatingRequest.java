package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotNull;

public record RatingRequest(@NotNull Boolean helpful) {
}
