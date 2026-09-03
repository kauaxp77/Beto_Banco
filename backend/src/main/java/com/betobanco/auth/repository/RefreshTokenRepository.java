package com.betobanco.auth.repository;

import com.betobanco.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :agora "
            + "WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revogarVigentesDe(@Param("userId") UUID userId, @Param("agora") Instant agora);

    /**
     * Sessoes vigentes, da mais antiga para a mais nova. Secao 10: "a terceira
     * sessao derruba a mais antiga" -- e por isso a ordem importa.
     */
    @Query("""
           SELECT t FROM RefreshToken t
            WHERE t.userId = :userId
              AND t.revokedAt IS NULL
              AND t.expiresAt > CURRENT_TIMESTAMP
            ORDER BY t.issuedAt ASC
           """)
    List<RefreshToken> vigentesDe(@Param("userId") UUID userId);

    /**
     * Secao 10: "Alerta automatico quando uma conta e acessada de mais de 4 IPs
     * distintos em 24h." Conta IP, e nao sessao: quatro logins do mesmo lugar
     * sao um aluno trocando de aba; quatro lugares diferentes sao uma senha
     * circulando em grupo de WhatsApp.
     */
    @Query("""
           SELECT COUNT(DISTINCT t.ip) FROM RefreshToken t
            WHERE t.userId = :userId AND t.ip IS NOT NULL AND t.issuedAt > :desde
           """)
    long contarIpsDistintosDesde(@Param("userId") UUID userId, @Param("desde") Instant desde);
}
