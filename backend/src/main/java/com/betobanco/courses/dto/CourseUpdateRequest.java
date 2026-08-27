package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseUpdateRequest(
        @NotBlank String title,
        String description,
        String coverUrl,
        boolean published) {
}
