package com.betobanco.auth.dto;

import java.util.Set;
import java.util.UUID;

public record MeResponse(UUID id, String email, String fullName, Set<String> roles) {
}
