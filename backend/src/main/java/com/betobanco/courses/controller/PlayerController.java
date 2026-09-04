package com.betobanco.courses.controller;

import com.betobanco.auth.api.ActiveSessions;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Protecao de conteudo no player. Documento Mestre V4.0, secao 10.
 *
 * <p>Secao 30 classifica pirataria como risco ALTO — "conteudo proprio e o
 * ativo" — e lista tres mitigacoes: marca d'agua com identificacao do aluno,
 * limite de dispositivos e alerta de uso anomalo. As duas ultimas vivem no
 * RefreshTokenService, aplicadas no login. Esta rota entrega a primeira.
 *
 * <p>A marca d'agua nao impede a copia; ela remove o anonimato de quem copia,
 * que e o que efetivamente desestimula o repasse em grupo.
 */
@RestController
@RequestMapping("/me/player")
@Tag(name = "Player")
public class PlayerController {

    private final UserDirectory usuarios;
    private final ActiveSessions sessoes;

    public PlayerController(UserDirectory usuarios, ActiveSessions sessoes) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
    }

    /**
     * Dados que o player sobrepoe ao video.
     *
     * <p>DESVIO REGISTRADO: a secao 10 pede "nome e CPF parcial do aluno". A
     * plataforma nao coleta CPF em lugar nenhum do schema atual, entao a
     * identificacao usa nome e e-mail mascarado. Fica igualmente pessoal e
     * igualmente rastreavel ate a conta; se a coleta de CPF entrar depois (ela
     * aparece na secao 22 como dado tratado), trocar aqui e uma linha.
     */
    @GetMapping("/watermark")
    @Operation(summary = "Identificação a sobrepor ao vídeo, para desestimular repasse")
    public ResponseEntity<ApiResponse<WatermarkResponse>> marcaDagua(
            @AuthenticationPrincipal AuthenticatedUser atual) {

        UserAccount conta = usuarios.buscarAtivoPorId(atual.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        return ResponseEntity.ok(ApiResponse.ok(new WatermarkResponse(
                conta.fullName(),
                mascarar(conta.email()),
                // Expira junto com o access token: um valor colado em outro
                // lugar deixa de conferir rapido, e o player pede outro.
                Instant.now().plus(Duration.ofMinutes(15)))));
    }

    /**
     * Sessoes ativas da conta, para o aluno ver de onde entrou.
     *
     * <p>Secao 10 limita a dois dispositivos; mostrar quais sao transforma a
     * regra em algo compreensivel, em vez de um logout inexplicado.
     */
    @GetMapping("/sessions")
    @Operation(summary = "Sessões ativas da conta e de onde foram abertas")
    public ResponseEntity<ApiResponse<List<SessaoResponse>>> sessoes(
            @AuthenticationPrincipal AuthenticatedUser atual) {

        List<SessaoResponse> ativas = sessoes.vigentesDe(atual.id()).stream()
                .map(s -> new SessaoResponse(s.ip(), s.userAgent(),
                        s.issuedAt(), s.expiresAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(ativas));
    }

    /** ana.silva@exemplo.com -> an*******@exemplo.com */
    private String mascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 2) {
            return email;
        }
        return email.charAt(0) + "*".repeat(arroba - 2) + email.charAt(arroba - 1)
                + email.substring(arroba);
    }

    public record WatermarkResponse(String name, String maskedEmail, Instant expiresAt) {
    }

    public record SessaoResponse(String ip, String device, Instant issuedAt, Instant expiresAt) {
    }
}
