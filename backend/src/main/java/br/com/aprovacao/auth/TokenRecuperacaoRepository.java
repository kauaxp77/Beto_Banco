package br.com.aprovacao.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRecuperacaoRepository extends JpaRepository<TokenRecuperacao, UUID> {

    Optional<TokenRecuperacao> findByTokenHash(String tokenHash);

    /** Pedir um novo link invalida os anteriores: so o ultimo e-mail funciona. */
    @Modifying
    @Query("""
           UPDATE TokenRecuperacao t
              SET t.usadoEm = :agora
            WHERE t.usuarioId = :usuarioId AND t.usadoEm IS NULL
           """)
    int invalidarPendentes(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);
}
