package com.betobanco.auth.service;

import com.betobanco.auth.entity.PasswordResetToken;
import com.betobanco.auth.entity.TokenPurpose;
import com.betobanco.auth.repository.PasswordResetTokenRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final int BYTES = 32;
    private static final String LINK_INVALIDO = "Link inválido ou expirado";

    private final PasswordResetTokenRepository repo;
    private final UserDirectory users;
    private final RefreshTokenService refreshTokens;
    private final Duration validadePrimeiroAcesso;
    private final Duration validadeReset;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository repo,
            UserDirectory users,
            RefreshTokenService refreshTokens,
            @Value("${betobanco.auth.first-access-token-hours}") long horasPrimeiro,
            @Value("${betobanco.auth.reset-token-hours}") long horasReset) {
        this.repo = repo;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.validadePrimeiroAcesso = Duration.ofHours(horasPrimeiro);
        this.validadeReset = Duration.ofHours(horasReset);
    }

    @Transactional
    public String criarToken(UserAccount usuario, TokenPurpose purpose) {
        Duration validade = purpose == TokenPurpose.FIRST_ACCESS
                ? validadePrimeiroAcesso
                : validadeReset;

        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repo.saveAndFlush(new PasswordResetToken(
                usuario.id(), hash(valor), purpose, Instant.now().plus(validade)));

        return valor;
    }

    @Transactional
    public void redefinir(String valorEmClaro, String novaSenha) {
        if (valorEmClaro == null || valorEmClaro.isBlank()) {
            throw new BusinessException(ErrorCode.CLIENT_ERROR, LINK_INVALIDO);
        }

        PasswordResetToken token = repo.findByTokenHash(hash(valorEmClaro))
                .filter(PasswordResetToken::estaVigente)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_ERROR, LINK_INVALIDO));

        users.redefinirSenha(token.getUserId(), novaSenha);

        token.marcarUsado();
        repo.saveAndFlush(token);

        // Quem redefine a senha normalmente esta reagindo a um acesso indevido.
        // Deixar sessoes antigas vivas anularia o proposito da redefinicao.
        refreshTokens.revogarTodosDe(token.getUserId());
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
