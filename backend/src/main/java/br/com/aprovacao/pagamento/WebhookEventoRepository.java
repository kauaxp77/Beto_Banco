package br.com.aprovacao.pagamento;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookEventoRepository extends JpaRepository<WebhookEvento, UUID> {

    /** Secao 12 -- idempotencia: evento repetido devolve 200 sem reprocessar. */
    Optional<WebhookEvento> findByGatewayAndEventoId(String gateway, String eventoId);

    /**
     * Ordena por ocorrido_em, nao por recebido_em: eventos podem chegar fora de
     * ordem (secao 12), e processar um cancelamento antes da aprovacao que o
     * precede deixaria o pedido no estado errado.
     */
    @Query("""
           SELECT w FROM WebhookEvento w
            WHERE w.status IN :status
              AND (w.proximaEm IS NULL OR w.proximaEm <= :agora)
            ORDER BY w.ocorridoEm ASC NULLS FIRST, w.recebidoEm ASC
           """)
    List<WebhookEvento> proximosDaFila(@Param("status") Collection<WebhookEvento.Status> status,
                                       @Param("agora") Instant agora,
                                       Limit limite);

    long countByStatus(WebhookEvento.Status status);
}
