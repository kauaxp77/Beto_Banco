package br.com.aprovacao.consumo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatriculaRepository extends JpaRepository<Matricula, UUID> {

    List<Matricula> findByPedidoId(UUID pedidoId);

    @Query("""
           SELECT m FROM Matricula m
            WHERE m.usuarioId = :usuarioId
              AND m.excluidoEm IS NULL
            ORDER BY m.expiraEm DESC
           """)
    List<Matricula> listarDoUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
           SELECT m FROM Matricula m
            WHERE m.usuarioId = :usuarioId
              AND m.cursoId = :cursoId
              AND m.excluidoEm IS NULL
            ORDER BY m.expiraEm DESC
            LIMIT 1
           """)
    Optional<Matricula> maisRecente(@Param("usuarioId") UUID usuarioId, @Param("cursoId") UUID cursoId);

    /**
     * Secao 12 -- EXPIRADO: "Fim dos 12 meses. Revoga com oferta de renovacao."
     * Rodado por job diario; o filtro de status evita reescrever linha ja revogada.
     */
    @Modifying
    @Query("""
           UPDATE Matricula m
              SET m.status = br.com.aprovacao.consumo.Matricula.Status.EXPIRADA
            WHERE m.status = br.com.aprovacao.consumo.Matricula.Status.ATIVA
              AND m.expiraEm <= :agora
           """)
    int expirarVencidas(@Param("agora") Instant agora);
}
