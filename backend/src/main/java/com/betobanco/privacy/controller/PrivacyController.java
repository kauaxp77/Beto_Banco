package com.betobanco.privacy.controller;

import com.betobanco.privacy.entity.Consent;
import com.betobanco.privacy.service.ConsentService;
import com.betobanco.privacy.service.DataSubjectService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Portal do titular. Documento Mestre V4.0, secao 22.
 *
 * <p>"Portal do titular no perfil: exportar meus dados (JSON), corrigir,
 * revogar consentimento, excluir conta." Autoatendimento, nao pedido por
 * e-mail — e o documento manda isso na Fase 1, nao depois.
 *
 * <p>A identidade sai sempre do token, nunca do corpo da requisicao: aceitar um
 * id de usuario vindo do cliente transformaria este controller em um jeito de
 * exportar ou apagar os dados de qualquer pessoa.
 */
@RestController
@RequestMapping("/me/privacy")
@Tag(name = "Privacidade")
public class PrivacyController {

    private static final Set<String> FINALIDADES = Set.of(
            Consent.MARKETING_WHATSAPP, Consent.MARKETING_EMAIL,
            Consent.COOKIE_ANALYTICS, Consent.COOKIE_MARKETING);

    private final ConsentService consentimentos;
    private final DataSubjectService titular;

    public PrivacyController(ConsentService consentimentos, DataSubjectService titular) {
        this.consentimentos = consentimentos;
        this.titular = titular;
    }

    // ------------------------------------------------------------------
    // Consentimento
    // ------------------------------------------------------------------

    @GetMapping("/consents")
    @Operation(summary = "Situação atual de cada finalidade, e o histórico completo")
    public ResponseEntity<ApiResponse<ConsentimentosResponse>> consentimentos(
            @AuthenticationPrincipal AuthenticatedUser atual) {

        List<RegistroConsentimento> historico = consentimentos.historico(atual.id()).stream()
                .map(c -> new RegistroConsentimento(
                        c.getPurpose(), c.isGranted(), c.getAcceptedText(), c.getRecordedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(new ConsentimentosResponse(
                consentimentos.situacaoAtual(atual.id()), historico)));
    }

    @PostMapping("/consents")
    @Operation(summary = "Concede ou revoga uma finalidade, com registro de data, hora e IP")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> registrar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody ConsentimentoRequest req,
            HttpServletRequest http) {

        String finalidade = req.purpose().toUpperCase(Locale.ROOT);
        if (!FINALIDADES.contains(finalidade)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Finalidade desconhecida: " + req.purpose());
        }

        consentimentos.registrar(atual.id(), finalidade, req.granted(), req.acceptedText(), ip(http));
        return ResponseEntity.ok(ApiResponse.ok(consentimentos.situacaoAtual(atual.id())));
    }

    @DeleteMapping("/consents")
    @Operation(summary = "Revoga todos os consentimentos de uma vez")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> revogarTudo(
            @AuthenticationPrincipal AuthenticatedUser atual, HttpServletRequest http) {

        int revogados = titular.revogarTodosOsConsentimentos(atual.id(), ip(http));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("revogados", revogados)));
    }

    // ------------------------------------------------------------------
    // Direitos do titular
    // ------------------------------------------------------------------

    @GetMapping("/export")
    @Operation(summary = "Exporta em JSON todos os dados pessoais do titular")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportar(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(titular.exportar(atual.id())));
    }

    @GetMapping("/requests")
    @Operation(summary = "Histórico de pedidos do titular sobre os próprios dados")
    public ResponseEntity<ApiResponse<List<PedidoResponse>>> pedidos(
            @AuthenticationPrincipal AuthenticatedUser atual) {

        List<PedidoResponse> pedidos = titular.pedidosDe(atual.id()).stream()
                .map(p -> new PedidoResponse(p.getType(), p.getStatus(), p.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(pedidos));
    }

    /**
     * Exclusao da conta. Secao 22: anonimiza o cadastro e preserva o registro
     * fiscal do pedido.
     *
     * <p>Exige a confirmacao textual porque a acao e irreversivel e nao ha
     * "desfazer": um DELETE disparado por engano em um clique de curiosidade
     * apagaria o historico de estudo de um aluno pagante.
     */
    @DeleteMapping("/account")
    @Operation(summary = "Exclui a conta: anonimiza o cadastro e preserva o registro fiscal")
    public ResponseEntity<ApiResponse<Map<String, String>>> excluirConta(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody ExclusaoRequest req) {

        if (!"EXCLUIR MINHA CONTA".equals(req.confirmation())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Para confirmar, envie confirmation com o texto exato: EXCLUIR MINHA CONTA");
        }

        titular.excluirConta(atual.id());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "anonimizado",
                "observacao", "O registro fiscal dos pagamentos foi preservado por obrigação legal.")));
    }

    // ------------------------------------------------------------------

    /**
     * Atras da Vercel e do Render o IP real vem em X-Forwarded-For; o primeiro
     * elemento e o cliente, os seguintes sao os proxies.
     */
    private String ip(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        return (encaminhado != null && !encaminhado.isBlank())
                ? encaminhado.split(",")[0].trim()
                : req.getRemoteAddr();
    }

    public record ConsentimentoRequest(
            @NotBlank String purpose,
            boolean granted,
            @NotBlank(message = "Registre o texto exato que foi exibido ao titular")
            String acceptedText) {
    }

    public record ExclusaoRequest(@NotBlank String confirmation) {
    }

    public record ConsentimentosResponse(Map<String, Boolean> atual,
                                         List<RegistroConsentimento> historico) {
    }

    public record RegistroConsentimento(String purpose, boolean granted,
                                        String acceptedText, Instant recordedAt) {
    }

    public record PedidoResponse(String type, String status, Instant createdAt) {
    }
}
