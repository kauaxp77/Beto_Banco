package com.betobanco.courses.controller;

import com.betobanco.courses.dto.LessonCardResponse;
import com.betobanco.courses.service.EngagementService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * "Continue assistindo", historico e favoritos. Documento Mestre Premium V3.0,
 * secao 5.
 *
 * <p>Controller separado do {@link StudentCourseController} porque sao eixos
 * diferentes: la o aluno navega o catalogo do curso; aqui ele volta ao proprio
 * rastro. Compartilham o prefixo /courses so porque o objeto e o mesmo.
 */
@RestController
@RequestMapping("/courses")
@Tag(name = "Engajamento do aluno")
public class StudentEngagementController {

    private final EngagementService engajamento;

    public StudentEngagementController(EngagementService engajamento) {
        this.engajamento = engajamento;
    }

    @PutMapping("/lessons/{lessonId}/playback")
    @Operation(summary = "Grava onde o aluno parou no vídeo")
    public ResponseEntity<ApiResponse<Void>> marcarPosicao(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody PosicaoRequest req) {

        engajamento.marcarPosicao(atual.id(), lessonId, req.positionSeconds());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me/continue")
    @Operation(summary = "Aulas começadas e ainda não concluídas, da mais recente para a mais antiga")
    public ResponseEntity<ApiResponse<List<CartaoResponse>>> continuar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @RequestParam(required = false) Integer limit) {

        return ResponseEntity.ok(ApiResponse.ok(
                engajamento.continuarAssistindo(atual.id(), limit).stream()
                        .map(CartaoResponse::de).toList()));
    }

    @GetMapping("/me/history")
    @Operation(summary = "Histórico de aulas abertas")
    public ResponseEntity<ApiResponse<List<CartaoResponse>>> historico(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @RequestParam(required = false) Integer limit) {

        return ResponseEntity.ok(ApiResponse.ok(
                engajamento.historico(atual.id(), limit).stream()
                        .map(CartaoResponse::de).toList()));
    }

    @GetMapping("/me/favorites")
    @Operation(summary = "Aulas favoritadas")
    public ResponseEntity<ApiResponse<List<CartaoResponse>>> favoritos(
            @AuthenticationPrincipal AuthenticatedUser atual) {

        return ResponseEntity.ok(ApiResponse.ok(
                engajamento.favoritos(atual.id()).stream().map(CartaoResponse::de).toList()));
    }

    @PostMapping("/lessons/{lessonId}/favorite")
    @Operation(summary = "Marca a aula como favorita")
    public ResponseEntity<ApiResponse<Void>> favoritar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId) {

        engajamento.favoritar(atual.id(), lessonId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/lessons/{lessonId}/favorite")
    @Operation(summary = "Desmarca a aula como favorita")
    public ResponseEntity<ApiResponse<Void>> desfavoritar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId) {

        engajamento.desfavoritar(atual.id(), lessonId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ------------------------------------------------------------------

    public record PosicaoRequest(
            @PositiveOrZero(message = "A posição do vídeo não pode ser negativa")
            int positionSeconds) {
    }

    /** O percentual sai calculado para as tres telas mostrarem o mesmo numero. */
    public record CartaoResponse(UUID lessonId, String lessonTitle, UUID courseId,
                                 String courseTitle, Integer durationSeconds,
                                 Integer positionSeconds, int percent, String at) {

        static CartaoResponse de(LessonCardResponse c) {
            return new CartaoResponse(c.lessonId(), c.lessonTitle(), c.courseId(),
                    c.courseTitle(), c.durationSeconds(), c.positionSeconds(),
                    c.percentual(), String.valueOf(c.at()));
        }
    }
}
