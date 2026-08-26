package com.betobanco.auth.controller;

import com.betobanco.auth.dto.ForgotPasswordRequest;
import com.betobanco.auth.dto.LoginRequest;
import com.betobanco.auth.dto.MeResponse;
import com.betobanco.auth.dto.RefreshRequest;
import com.betobanco.auth.dto.RegisterRequest;
import com.betobanco.auth.dto.ResetPasswordRequest;
import com.betobanco.auth.dto.TokenResponse;
import com.betobanco.auth.entity.TokenPurpose;
import com.betobanco.auth.service.AuthService;
import com.betobanco.auth.service.PasswordResetService;
import com.betobanco.auth.service.RefreshTokenService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService auth;
    private final UserDirectory users;
    private final RefreshTokenService refreshTokens;
    private final PasswordResetService resets;

    public AuthController(AuthService auth, UserDirectory users,
                          RefreshTokenService refreshTokens, PasswordResetService resets) {
        this.auth = auth;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resets = resets;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(auth.autenticar(req.email(), req.password())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        // A identidade vem do token, nunca do cliente.
        UserAccount conta = users.buscarAtivoPorId(atual.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        return ResponseEntity.ok(ApiResponse.ok(new MeResponse(
                conta.id(), conta.email(), conta.fullName(), conta.roles())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest req) {
        RefreshTokenService.Rotacao rotacao = refreshTokens.rotacionar(req.refreshToken())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED, "Sessão inválida ou expirada"));

        return ResponseEntity.ok(ApiResponse.ok(
                auth.emitirParComRefreshExistente(rotacao.usuario(), rotacao.novoValor())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        // Sempre 204: responder 404 revelaria quais tokens existem.
        refreshTokens.revogar(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MeResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        UserAccount criado = users.registrar(req.email(), req.password(), req.fullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(new MeResponse(
                criado.id(), criado.email(), criado.fullName(), criado.roles())));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        // Resposta identica exista o e-mail ou nao: qualquer diferenca
        // transformaria o endpoint num enumerador de clientes.
        users.buscarPorEmail(req.email())
                .ifPresent(u -> resets.criarToken(u, TokenPurpose.RESET));

        // O envio do e-mail entra na Fase 3, junto com a outbox.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        resets.redefinir(req.token(), req.password());
        return ResponseEntity.noContent().build();
    }
}
