package com.betobanco.privacy.controller;

import com.betobanco.privacy.entity.LegalDocument;
import com.betobanco.privacy.service.ConsentService;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Textos legais vigentes. Documento Mestre V4.0, secao 22.
 *
 * <p>Publico de proposito: o checkout precisa exibir os termos antes do aceite,
 * e um texto legal atras de login nao cumpre o proposito de informar.
 */
@RestController
@RequestMapping("/legal")
@Tag(name = "Legal")
public class LegalDocumentController {

    /** Aceita o tipo em kebab-case na URL, que e o que fica legivel para o leitor. */
    private static final Set<String> TIPOS = Set.of(
            LegalDocument.TERMS_OF_USE, LegalDocument.PRIVACY_POLICY, LegalDocument.COOKIE_POLICY);

    private final ConsentService consentimentos;

    public LegalDocumentController(ConsentService consentimentos) {
        this.consentimentos = consentimentos;
    }

    @GetMapping("/{type}")
    @Operation(summary = "Texto vigente de termos de uso, política de privacidade ou de cookies")
    public ResponseEntity<ApiResponse<DocumentoResponse>> vigente(@PathVariable("type") String type) {
        String normalizado = type.toUpperCase(Locale.ROOT).replace('-', '_');
        if (!TIPOS.contains(normalizado)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tipo de documento inválido. Use terms-of-use, privacy-policy ou cookie-policy.");
        }

        LegalDocument documento = consentimentos.vigente(normalizado);
        return ResponseEntity.ok(ApiResponse.ok(new DocumentoResponse(
                documento.getType(), documento.getVersion(),
                documento.getBody(), documento.getEffectiveFrom())));
    }

    public record DocumentoResponse(String type, String version, String body, Instant effectiveFrom) {
    }
}
