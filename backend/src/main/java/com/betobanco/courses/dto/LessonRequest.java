package com.betobanco.courses.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record LessonRequest(
        @NotBlank String title,
        String description,
        String videoUrl,
        @Min(0) Integer durationSeconds,
        int position,
        Boolean published) {
}
