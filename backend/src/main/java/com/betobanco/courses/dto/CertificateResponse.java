package com.betobanco.courses.dto;

import java.time.Instant;
import java.util.UUID;

/** Dados do certificado — os mesmos para o dono e para quem valida o codigo. */
public record CertificateResponse(
        String code,
        String studentName,
        UUID courseId,
        String courseTitle,
        int hours,
        Instant issuedAt) {
}
