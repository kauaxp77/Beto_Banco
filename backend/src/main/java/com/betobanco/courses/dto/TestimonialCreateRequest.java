package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TestimonialCreateRequest(
        @NotBlank @Size(max = 2000) String body,
        UUID courseId) {
}
