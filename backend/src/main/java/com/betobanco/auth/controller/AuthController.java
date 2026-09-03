package com.betobanco.auth.controller;

import com.betobanco.auth.dto.ForgotPasswordRequest;
import com.betobanco.auth.dto.LoginRequest;
import com.betobanco.auth.dto.MeResponse;
import com.betobanco.auth.dto.RegisterRequest;
import com.betobanco.auth.dto.ResetPasswordRequest;
import com.betobanco.auth.dto.TokenResponse;
import com.betobanco.auth.service.AuthService;
import com.betobanco.auth.service.PasswordResetService;
import com.betobanco.auth.service.RefreshCookies;
import com.betobanco.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final RefreshCookies cookies;

    public AuthController(AuthService auth, UserDirectory users,
                          RefreshTokenService refreshTokens, PasswordResetService resets,
                          RefreshCookies cookies) {
        this.auth = auth;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resets = resets;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest req,
                                                            HttpServletRequest http) {
        TokenResponse par = auth.autenticar(req.email(), req.password(), origemDe(http));

        // O refresh vai no cookie HttpOnly; o JSON leva apenas o access token.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.emitir(par.refreshToken()).toString())
                .body(ApiResponse.ok(par));
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
            @CookieValue(value = RefreshCookies.NOME, required = false) String refresh) {
        if (refresh == null || refresh.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Sessão inválida ou expirada");
        }

        RefreshTokenService.Rotacao rotacao = refreshTokens.rotacionar(refresh)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED, "Sessão inválida ou expirada"));

        TokenResponse par =
                auth.emitirParComRefreshExistente(rotacao.usuario(), rotacao.novoValor());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.emitir(rotacao.novoValor()).toString())
                .body(ApiResponse.ok(par));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = RefreshCookies.NOME, required = false) String refresh) {
        // Sempre 204: responder 404 revelaria quais tokens existem.
        if (refresh != null && !refresh.isBlank()) {
            refreshTokens.revogar(refresh);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.limpar().toString())
                .build();
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
        users.buscarPorEmail(req.email()).ifPresent(resets::solicitarRecuperacao);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        resets.redefinir(req.token(), req.password());
        return ResponseEntity.noContent().build();
    }

    /**
     * Secao 10 -- de onde a sessao nasceu.
     *
     * <p>Atras da Vercel e do Render o IP real vem em X-Forwarded-For; o
     * primeiro elemento e o cliente e os seguintes sao os proxies. Ler
     * getRemoteAddr() direto registraria o IP do proxy para todo mundo, e a
     * contagem de IPs distintos da secao 10 nunca dispararia.
     */
    private RefreshTokenService.Origem origemDe(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        String ip = (encaminhado != null && !encaminhado.isBlank())
                ? encaminhado.split(",")[0].trim()
                : req.getRemoteAddr();
        return new RefreshTokenService.Origem(ip, req.getHeader("User-Agent"));
    }
}
