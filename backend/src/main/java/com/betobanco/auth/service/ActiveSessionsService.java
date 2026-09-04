package com.betobanco.auth.service;

import com.betobanco.auth.api.ActiveSessions;
import com.betobanco.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Implementacao de {@link ActiveSessions}: unico ponto que traduz o token de renovacao em tela. */
@Service
public class ActiveSessionsService implements ActiveSessions {

    private final RefreshTokenRepository tokens;

    public ActiveSessionsService(RefreshTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessaoAtiva> vigentesDe(UUID userId) {
        return tokens.vigentesDe(userId).stream()
                .map(t -> new SessaoAtiva(t.getIp(), t.getUserAgent(),
                        t.getIssuedAt(), t.getExpiresAt()))
                .toList();
    }
}
