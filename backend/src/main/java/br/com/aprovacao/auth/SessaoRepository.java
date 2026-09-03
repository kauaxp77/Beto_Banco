package br.com.aprovacao.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

    Optional<Sessao> findByRefreshTokenHash(String refreshTokenHash);

    @Query("""
           SELECT s FROM Sessao s
            WHERE s.usuarioId = :usuarioId
              AND s.revogadoEm IS NULL
              AND s.expiraEm > CURRENT_TIMESTAMP
            ORDER BY s.criadoEm ASC
           """)
    List<Sessao> listarVivas(@Param("usuarioId") UUID usuarioId);

    /**
     * Secao 20: "Reuso de refresh invalida toda a familia de tokens daquele
     * dispositivo." Uma unica sentenca porque a corrida entre o atacante e o dono
     * legitimo se decide em milissegundos.
     */
    @Modifying
    @Query("""
           UPDATE Sessao s
              SET s.revogadoEm = :agora, s.motivoRevogacao = :motivo
            WHERE s.familiaId = :familiaId AND s.revogadoEm IS NULL
           """)
    int revogarFamilia(@Param("familiaId") UUID familiaId,
                       @Param("agora") Instant agora,
                       @Param("motivo") String motivo);

    @Modifying
    @Query("""
           UPDATE Sessao s
              SET s.revogadoEm = :agora, s.motivoRevogacao = 'LOGOUT_GLOBAL'
            WHERE s.usuarioId = :usuarioId AND s.revogadoEm IS NULL
           """)
    int revogarTodasDoUsuario(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);
}
