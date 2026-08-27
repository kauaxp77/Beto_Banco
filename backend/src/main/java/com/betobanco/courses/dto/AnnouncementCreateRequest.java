package com.betobanco.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AnnouncementCreateRequest(
        UUID courseId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 8000) String body,
        Boolean sendEmail) {
}
