package com.betobanco.users.dto;

import java.util.UUID;

public record StudentResponse(UUID id, String email, String fullName, String phone) {
}
