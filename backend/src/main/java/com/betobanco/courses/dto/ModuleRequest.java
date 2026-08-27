package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record ModuleRequest(
        @NotBlank String title,
        int position) {
}
