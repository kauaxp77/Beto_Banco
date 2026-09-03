package com.betobanco.essays.controller;

import com.betobanco.essays.entity.Essay;
import com.betobanco.essays.entity.EssayCorrection;
import com.betobanco.essays.service.EssayService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fila e devolutiva do corretor. Documento Mestre V4.0, secao 14.
 *
 * <p>Secao 14: "Corretor humano com perfil proprio. IA so sugere; nao publica
 * nota." O perfil e separado de INSTRUCTOR porque corrigir redacao da acesso ao
 * texto de um aluno identificado — isso nao deve vir de brinde com "pode editar
 * aula".
 */
@RestController
@RequestMapping("/corrections")
@PreAuthorize("hasAnyRole('CORRECTOR', 'ADMIN')")
@Tag(name = "Correção de redações")
public class CorrectionController {

    private final EssayService essays;

    public CorrectionController(EssayService essays) {
        this.essays = essays;
    }

    @GetMapping("/queue")
    @Operation(summary = "Fila de correção, do prazo mais próximo de vencer para o mais distante")
    public ResponseEntity<ApiResponse<List<ItemDaFila>>> fila(
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(
                essays.fila(limit).stream().map(ItemDaFila::de).toList()));
    }

    @PostMapping("/{essayId}/claim")
    @Operation(summary = "Assume uma redação da fila")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assumir(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable UUID essayId) {

        EssayCorrection correcao = essays.assumir(essayId, atual.id());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "correctionId", correcao.getId(),
                "rubricId", String.valueOf(correcao.getRubricId()))));
    }

    @PostMapping("/{essayId}/publish")
    @Operation(summary = "Publica a devolutiva: nota por critério, comentário e anotações")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publicar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable UUID essayId,
            @Valid @RequestBody DevolutivaRequest req) {

        EssayCorrection correcao = essays.publicarDevolutiva(
                essayId, atual.id(), req.scores(), req.comment(), req.audioUrl(), req.annotations());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalScore", String.valueOf(correcao.getTotalScore()),
                "completedAt", String.valueOf(correcao.getCompletedAt()))));
    }

    // ------------------------------------------------------------------

    public record DevolutivaRequest(
            @NotEmpty(message = "Informe a nota de cada critério da rubrica")
            Map<String, BigDecimal> scores,
            String comment,
            String audioUrl,
            /** JSON de anotações sobre o texto; opcional. */
            String annotations) {
    }

    public record ItemDaFila(UUID id, String prompt, String board, String status,
                             Instant submittedAt, Instant dueAt, long diasRestantes,
                             boolean vencida) {

        static ItemDaFila de(Essay e) {
            long dias = e.diasRestantes();
            // O corretor precisa ver o que ja estourou o prazo destacado, nao
            // apenas no topo da lista: no topo tambem esta o que vence amanha.
            return new ItemDaFila(e.getId(), e.getPrompt(), e.getBoard(), e.getStatus(),
                    e.getSubmittedAt(), e.getDueAt(), dias, dias < 0);
        }
    }
}
