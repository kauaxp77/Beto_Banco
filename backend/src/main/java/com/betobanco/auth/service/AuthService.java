package com.betobanco.auth.service;

import com.betobanco.auth.dto.TokenResponse;
import com.betobanco.security.JwtService;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    /**
     * Mensagem unica para qualquer falha de login. Distinguir "e-mail nao
     * existe" de "senha errada" transformaria o endpoint num enumerador de
     * clientes.
     */
    private static final String CREDENCIAIS_INVALIDAS = "Credenciais inválidas";

    private final UserDirectory users;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserDirectory users, JwtService jwt, RefreshTokenService refreshTokens) {
        this.users = users;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public TokenResponse autenticar(String email, String senha) {
        return autenticar(email, senha, RefreshTokenService.Origem.DESCONHECIDA);
    }

    /**
     * Secao 10 -- a origem da sessao (IP e dispositivo) e o que permite limitar
     * dispositivos simultaneos e detectar conta compartilhada.
     */
    @Transactional
    public TokenResponse autenticar(String email, String senha, RefreshTokenService.Origem origem) {
        Optional<UserAccount> conta = users.verificarCredenciais(email, senha);

        if (conta.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        return emitirPar(conta.get(), origem);
    }

    /** Emite um par completo de tokens para um usuario ja autenticado. */
    @Transactional
    public TokenResponse emitirPar(UserAccount usuario) {
        return emitirPar(usuario, RefreshTokenService.Origem.DESCONHECIDA);
    }

    @Transactional
    public TokenResponse emitirPar(UserAccount usuario, RefreshTokenService.Origem origem) {
        String access = jwt.gerar(usuario.id(), usuario.email(), usuario.roles());
        String refresh = refreshTokens.emitir(usuario, origem);
        return TokenResponse.bearer(access, refresh, jwt.duracaoSegundos());
    }

    /**
     * Emite um access token para um refresh que ACABOU de ser rotacionado.
     * Diferente de {@link #emitirPar}, nao cria outro refresh: isso deixaria
     * dois tokens vigentes por rotacao e a cadeia deixaria de ser uma cadeia.
     */
    public TokenResponse emitirParComRefreshExistente(UserAccount usuario, String refreshJaEmitido) {
        String access = jwt.gerar(usuario.id(), usuario.email(), usuario.roles());
        return TokenResponse.bearer(access, refreshJaEmitido, jwt.duracaoSegundos());
    }
}
