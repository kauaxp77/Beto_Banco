package com.betobanco.contests.controller;

import com.betobanco.contests.entity.Contest;
import com.betobanco.contests.service.ContestService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administracao das fichas de concurso. Documento Mestre V4.0, secao 11.
 *
 * <p>A rota fica sob /admin, entao o SecurityConfig ja exige ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin/contests")
@Tag(name = "Admin - Concursos")
public class AdminContestController {

    private final ContestService contests;

    public AdminContestController(ContestService contests) {
        this.contests = contests;
    }

    /**
     * A fila de revisao da secao 11.
     *
     * <p>"Ficha sem verificacao ha mais de 60 dias entra em fila de revisao no
     * admin." E o mecanismo que impede o catalogo de envelhecer em silencio: um
     * salario correto hoje fica errado quando sai o reajuste, e ninguem avisa.
     */
    @GetMapping("/review-queue")
    @Operation(summary = "Fichas sem verificação há mais de 60 dias, da mais antiga para a mais recente")
    public ResponseEntity<ApiResponse<List<PendenteResponse>>> filaDeRevisao(
            @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(contests.filaDeRevisao(limit).stream()
                .map(PendenteResponse::de).toList()));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Registra que a ficha foi conferida contra a fonte oficial")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verificar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable UUID id,
            @Valid @RequestBody VerificacaoRequest req) {

        Contest contest = contests.registrarVerificacao(id, atual.id(), req.sourceUrl());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "verifiedAt", String.valueOf(contest.getVerifiedAt()),
                "sourceUrl", contest.getSourceUrl())));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publica a ficha; exige verificação e fonte oficial")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publicar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable UUID id) {

        Contest contest = contests.publicar(id, atual.id());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "publishedAt", String.valueOf(contest.getPublishedAt()),
                "slug", contest.getSlug())));
    }

    /** Secao 07 — um concurso pode pertencer a mais de uma carreira. */
    @PutMapping("/{id}/careers")
    @Operation(summary = "Define em quais carreiras o concurso aparece")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> definirCarreiras(
            @PathVariable UUID id,
            @Valid @RequestBody CarreirasRequest req) {

        contests.definirCarreiras(id, req.careerIds());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("careers", req.careerIds().size())));
    }

    // ------------------------------------------------------------------

    public record VerificacaoRequest(
            @NotBlank(message = "Informe o link da fonte oficial conferida")
            String sourceUrl) {
    }

    public record CarreirasRequest(
            @NotEmpty(message = "Informe ao menos uma carreira")
            List<UUID> careerIds) {
    }

    public record PendenteResponse(UUID id, String name, String slug, String status,
                                   Instant verifiedAt, long daysSinceVerification,
                                   boolean published) {

        static PendenteResponse de(Contest c) {
            return new PendenteResponse(c.getId(), c.getName(), c.getSlug(), c.getStatus(),
                    c.getVerifiedAt(), c.diasDesdeVerificacao(), c.publicado());
        }
    }
}
