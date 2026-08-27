package com.betobanco.invites.controller;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.auth.api.FirstAccessTokens;
import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.email.api.EmailService;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.invites.dto.InviteRequest;
import com.betobanco.invites.dto.InviteResponse;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Convites de cortesia: acesso sem pagamento (bolsista, parceiro, cortesia).
 * Mesmo fluxo do webhook aprovado — cria a conta sem senha se preciso e
 * envia o link de primeiro acesso — mas disparado pelo admin, com validade
 * opcional. Modulo sem tabela propria: consome apenas as api/ dos demais.
 */
@RestController
@RequestMapping("/admin/invites")
@Tag(name = "Admin - Invites")
public class AdminInviteController {

    /** Prefixo do sourceRef que marca concessoes vindas de convite. */
    static final String PREFIXO = "INVITE:";

    private final UserDirectory usuarios;
    private final ProductCatalog catalogo;
    private final EntitlementService entitlements;
    private final EmailService emails;
    private final FirstAccessTokens primeiroAcesso;
    private final AuditLogger auditoria;

    public AdminInviteController(UserDirectory usuarios, ProductCatalog catalogo,
                                 EntitlementService entitlements, EmailService emails,
                                 FirstAccessTokens primeiroAcesso, AuditLogger auditoria) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.entitlements = entitlements;
        this.emails = emails;
        this.primeiroAcesso = primeiroAcesso;
        this.auditoria = auditoria;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<InviteResponse>> convidar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody InviteRequest req) {

        var produto = catalogo.buscarPorId(req.productId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        String email = req.email().trim().toLowerCase();
        boolean contaNova = usuarios.buscarPorEmail(email).isEmpty();
        UserAccount aluno = usuarios.buscarPorEmail(email)
                .orElseGet(() -> usuarios.criarSemSenha(email,
                        req.fullName() == null || req.fullName().isBlank()
                                ? email : req.fullName().trim()));

        Instant validade = (req.validadeDias() == null || req.validadeDias() == 0)
                ? null
                : Instant.now().plus(req.validadeDias(), ChronoUnit.DAYS);

        var concessao = entitlements.conceder(aluno.id(), produto.id(), "MANUAL",
                PREFIXO + atual.id(), validade);

        auditoria.registrar("INVITE_SENT", "Entitlement",
                concessao.entitlementId().toString(),
                Map.of("email", email, "productId", produto.id().toString(),
                        "expiresAt", validade == null ? "vitalicio" : validade.toString()));

        // Mesmo contrato de e-mail do fluxo de pagamento (outbox, com rollback).
        if (contaNova) {
            String token = primeiroAcesso.criarPara(aluno);
            emails.enfileirar(aluno.email(), EmailService.Templates.PRIMEIRO_ACESSO,
                    Map.of("nome", aluno.fullName(), "userId", aluno.id().toString(),
                            "token", token),
                    "primeiro-acesso:" + aluno.id());
        } else {
            emails.enfileirar(aluno.email(), EmailService.Templates.ACESSO_LIBERADO,
                    Map.of("nome", aluno.fullName(), "produto", produto.name()),
                    "acesso-liberado:" + concessao.entitlementId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                new InviteResponse(concessao.entitlementId(), aluno.email(), aluno.fullName(),
                        produto.id(), produto.name(), Instant.now(), validade, false,
                        contaNova)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<InviteResponse>>> listar() {
        List<InviteResponse> convites = entitlements.listarPorSourceRefPrefixo(PREFIXO).stream()
                .limit(100)
                .map(item -> {
                    Optional<UserAccount> dono = usuarios.buscarAtivoPorId(item.userId());
                    var produto = catalogo.buscarPorId(item.productId()).orElse(null);
                    return new InviteResponse(item.entitlementId(),
                            dono.map(UserAccount::email).orElse("—"),
                            dono.map(UserAccount::fullName).orElse("Conta removida"),
                            item.productId(),
                            produto == null ? null : produto.name(),
                            item.grantedAt(), item.expiresAt(),
                            item.revokedAt() != null, false);
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(convites));
    }
}
