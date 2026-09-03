package com.betobanco.essays.controller;

import com.betobanco.essays.entity.Essay;
import com.betobanco.essays.entity.EssayCorrection;
import com.betobanco.essays.entity.EssayQuota;
import com.betobanco.essays.service.EssayQuotaService;
import com.betobanco.essays.service.EssayService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Redacoes do aluno. Documento Mestre V4.0, secao 14.
 *
 * <p>O arquivo sobe direto para o storage e chega aqui como URL, no mesmo
 * padrao de {@code lesson_materials}: o binario nao passa pela API, que assim
 * nao vira gargalo de upload de 10 MB nem precisa de disco.
 */
@RestController
@RequestMapping("/me/essays")
@Tag(name = "Redações")
public class EssayController {

    private final EssayService essays;
    private final EssayQuotaService cotas;

    public EssayController(EssayService essays, EssayQuotaService cotas) {
        this.essays = essays;
        this.cotas = cotas;
    }

    @GetMapping("/quota")
    @Operation(summary = "Cota de correções da competência atual")
    public ResponseEntity<ApiResponse<CotaResponse>> cota(
            @AuthenticationPrincipal AuthenticatedUser atual) {

        EssayQuota cota = cotas.cotaAtual(atual.id());
        return ResponseEntity.ok(ApiResponse.ok(new CotaResponse(
                cota.getPeriod().toString(), cota.getAvailable(), cota.getUsed(), cota.restantes())));
    }

    @GetMapping
    @Operation(summary = "Minhas redações, com o prazo de cada uma")
    public ResponseEntity<ApiResponse<List<RedacaoResponse>>> minhas(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(
                essays.minhas(atual.id()).stream().map(RedacaoResponse::de).toList()));
    }

    @PostMapping
    @Operation(summary = "Envia uma redação para correção; consome uma cota")
    public ResponseEntity<ApiResponse<RedacaoResponse>> enviar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody EnvioRequest req) {

        Essay redacao = essays.enviar(atual.id(), req.prompt(), req.board(), req.fileUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(RedacaoResponse.de(redacao)));
    }

    @PostMapping("/{id}/rewrite")
    @Operation(summary = "Envia a reescrita de uma redação já corrigida; não consome cota")
    public ResponseEntity<ApiResponse<RedacaoResponse>> reescrever(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable UUID id,
            @Valid @RequestBody ReescritaRequest req) {

        Essay reescrita = essays.enviarReescrita(atual.id(), id, req.fileUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(RedacaoResponse.de(reescrita)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Uma redação e, se já publicada, a devolutiva")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detalhe(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable UUID id) {

        Essay redacao = essays.minha(atual.id(), id);
        Map<String, Object> corpo = new java.util.LinkedHashMap<>();
        corpo.put("redacao", RedacaoResponse.de(redacao));
        corpo.put("devolutiva", essays.devolutiva(id).map(DevolutivaResponse::de).orElse(null));
        return ResponseEntity.ok(ApiResponse.ok(corpo));
    }

    @GetMapping("/rubrics")
    @Operation(summary = "Rubricas ativas, para o aluno saber por quais critérios será avaliado")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> rubricas() {
        return ResponseEntity.ok(ApiResponse.ok(essays.rubricasAtivas().stream()
                .map(r -> Map.<String, Object>of(
                        "board", r.getBoard(), "name", r.getName(), "criteria", r.getCriteria()))
                .toList()));
    }

    // ------------------------------------------------------------------

    public record EnvioRequest(
            @NotBlank @Size(max = 500) String prompt,
            String board,
            @NotBlank String fileUrl) {
    }

    public record ReescritaRequest(@NotBlank String fileUrl) {
    }

    public record CotaResponse(String competencia, int total, int usadas, int restantes) {
    }

    public record RedacaoResponse(UUID id, String prompt, String board, String status,
                                  Instant submittedAt, Instant dueAt, long diasRestantes,
                                  UUID rewriteOf) {

        static RedacaoResponse de(Essay e) {
            return new RedacaoResponse(e.getId(), e.getPrompt(), e.getBoard(), e.getStatus(),
                    e.getSubmittedAt(), e.getDueAt(), e.diasRestantes(), e.getRewriteOf());
        }
    }

    public record DevolutivaResponse(String scores, BigDecimal totalScore, String comment,
                                     String audioUrl, String annotations, Instant completedAt) {

        static DevolutivaResponse de(EssayCorrection c) {
            // aiDraft nao entra: e rascunho para o corretor, e a secao 14 e
            // explicita em que a IA nao publica nota.
            return new DevolutivaResponse(c.getScores(), c.getTotalScore(), c.getComment(),
                    c.getAudioUrl(), c.getAnnotations(), c.getCompletedAt());
        }
    }
}
