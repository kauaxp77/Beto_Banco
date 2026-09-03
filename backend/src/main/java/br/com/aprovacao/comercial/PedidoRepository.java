package br.com.aprovacao.comercial;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    /** Secao 19 -- Idempotency-Key obrigatorio em todo POST que cria pedido. */
    Optional<Pedido> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    @Query("""
           SELECT p FROM Pedido p
            WHERE lower(p.email) = lower(:email)
              AND p.excluidoEm IS NULL
            ORDER BY p.criadoEm DESC
           """)
    List<Pedido> listarPorEmail(@Param("email") String email);

    /** Secao 12 -- PENDENTE expira em 72h. */
    @Query("""
           SELECT p FROM Pedido p
            WHERE p.status = br.com.aprovacao.comercial.StatusPedido.PENDENTE
              AND p.expiraEm <= :agora
              AND p.excluidoEm IS NULL
           """)
    List<Pedido> pendentesVencidos(@Param("agora") Instant agora);

    @Query("""
           SELECT p FROM Pedido p
            WHERE p.status = br.com.aprovacao.comercial.StatusPedido.APROVADO
              AND p.excluidoEm IS NULL
              AND p.atualizadoEm >= :desde
           """)
    List<Pedido> aprovadosDesde(@Param("desde") Instant desde);
}
