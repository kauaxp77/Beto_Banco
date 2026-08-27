package com.betobanco.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusUpdateRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|BLOCKED",
                message = "status deve ser ACTIVE ou BLOCKED")
        String status) {
}
