package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record MaterialRequest(
        @NotBlank String title,
        @NotBlank String url,
        int position) {
}
