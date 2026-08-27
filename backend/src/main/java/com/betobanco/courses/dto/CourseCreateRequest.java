package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseCreateRequest(
        @NotBlank String title,
        String description,
        String coverUrl) {
}
