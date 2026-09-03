package br.com.aprovacao.pagamento;

import br.com.aprovacao.comercial.Pedido;
import br.com.aprovacao.comercial.PedidoRepository;
import br.com.aprovacao.comercial.StatusPedido;
import br.com.aprovacao.config.PropriedadesPlataforma;
import br.com.aprovacao.lgpd.ServicoEmail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica um evento de gateway ao pedido correspondente, com as quatro garantias da
 * secao 12: idempotencia, tolerancia a evento fora de ordem, backoff e fila morta.
 */
@Service
public class AplicadorDeEventoWebhook {

    private static final Logger log = LoggerFactory.getLogger(AplicadorDeEventoWebhook.class);

    private final WebhookEventoRepository eventos;
    private final PedidoRepository pedidos;
    private final ServicoAcesso acesso;
    private final ServicoEmail email;
    private final PropriedadesPlataforma props;
    private final ObjectMapper json;

    public AplicadorDeEventoWebhook(WebhookEventoRepository eventos,
                                    PedidoRepository pedidos,
                                    ServicoAcesso acesso,
                                    ServicoEmail email,
                                    PropriedadesPlataforma props,
                                    ObjectMapper json) {
        this.eventos = eventos;
        this.pedidos = pedidos;
        this.acesso = acesso;
        this.email = email;
        this.props = props;
        this.json = json;
    }

    /**
     * REQUIRES_NEW: cada evento e uma transacao propria. Um evento defeituoso no
     * meio da varredura nao pode desfazer o acesso ja liberado pelos anteriores.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processar(UUID eventoId) {
        WebhookEvento evento = eventos.findById(eventoId).orElse(null);
        if (evento == null || evento.getStatus() == WebhookEvento.Status.PROCESSADO
                || evento.getStatus() == WebhookEvento.Status.IGNORADO
                || evento.getStatus() == WebhookEvento.Status.FILA_MORTA) {
            return;
        }

        try {
            aplicar(evento);
            evento.marcarProcessado();
        } catch (EventoIgnoravel ignoravel) {
            evento.marcarIgnorado(ignoravel.getMessage());
        } catch (RuntimeException e) {
            boolean caiuNaFilaMorta = evento.registrarFalha(e.toString(), props.pagamento().backoffMinutos());
            if (caiuNaFilaMorta) {
                // Secao 23: "Alertas para: webhook na fila morta".
                log.error("Evento {} ({}) foi para a fila morta apos {} tentativas.",
                        evento.getEventoId(), evento.getTipo(), evento.getTentativas(), e);
                email.alertarFinanceiro("Webhook na fila morta",
                        "Evento " + evento.getEventoId() + " do gateway falhou "
                                + evento.getTentativas() + " vezes. Ultimo erro: " + evento.getErro());
            } else {
                log.warn("Evento {} falhou (tentativa {}). Nova tentativa agendada.",
                        evento.getEventoId(), evento.getTentativas());
            }
        }
    }

    private void aplicar(WebhookEvento evento) {
        JsonNode payload = ler(evento.getPayloadJson());

        String referencia = texto(payload, "order_id", "reference", "external_reference", "pedido_id");
        if (referencia == null) {
            throw new EventoIgnoravel("Evento sem referencia de pedido. Nada a aplicar.");
        }

        Pedido pedido = buscarPedido(referencia)
                .orElseThrow(() -> new IllegalStateException(
                        "Pedido " + referencia + " nao encontrado. Pode ser ordem de chegada; sera retentado."));

        StatusPedido novo = traduzir(texto(payload, "status", "event", "type"));
        if (novo == null) {
            throw new EventoIgnoravel("Status do gateway sem equivalente na plataforma: "
                    + texto(payload, "status", "event", "type"));
        }

        // Secao 12: "Ordem: eventos podem chegar fora de ordem. Comparar ocorrido_em
        // e ignorar evento mais antigo que o estado atual."
        if (pedido.getStatus().ehFinal() && !novo.ehFinal()) {
            throw new EventoIgnoravel("Pedido ja em estado final (" + pedido.getStatus()
                    + "); evento " + novo + " chegou atrasado e foi descartado.");
        }

        if (!pedido.mudarStatus(novo)) {
            throw new EventoIgnoravel("Pedido ja estava em " + novo + ". Nada a fazer.");
        }

        if (novo.liberaAcesso()) {
            acesso.liberarAcesso(pedido);
        } else if (novo.revogaAcesso()) {
            acesso.revogarAcesso(pedido, novo);
        } else if (novo == StatusPedido.RECUSADO) {
            // Secao 12 -- recuperacao de venda: WhatsApp em 30 min, e-mail em 24h.
            email.enviarRecuperacaoDeVenda(pedido.getEmail(), pedido.getNome());
        }

        pedidos.save(pedido);
    }

    private Optional<Pedido> buscarPedido(String referencia) {
        try {
            return pedidos.findById(UUID.fromString(referencia));
        } catch (IllegalArgumentException naoEhUuid) {
            // O gateway pode devolver a nossa Idempotency-Key em vez do id.
            return pedidos.findByTenantIdAndIdempotencyKey(props.tenantPadrao(), referencia);
        }
    }

    /**
     * Vocabulario do gateway para os sete estados da secao 12. Um status novo do
     * lado do gateway devolve null e o evento vira IGNORADO com o motivo gravado --
     * visivel no admin, em vez de silenciosamente tratado como aprovacao.
     */
    private StatusPedido traduzir(String statusGateway) {
        if (statusGateway == null) {
            return null;
        }
        return switch (statusGateway.toLowerCase(Locale.ROOT)) {
            case "paid", "approved", "aprovado", "payment.approved", "compra_aprovada" -> StatusPedido.APROVADO;
            case "pending", "pendente", "waiting_payment"                              -> StatusPedido.PENDENTE;
            case "refused", "declined", "recusado", "payment.refused"                  -> StatusPedido.RECUSADO;
            case "canceled", "cancelled", "cancelado"                                  -> StatusPedido.CANCELADO;
            case "refunded", "estornado", "payment.refunded"                           -> StatusPedido.ESTORNADO;
            case "chargeback", "dispute", "payment.chargeback"                         -> StatusPedido.CHARGEBACK;
            case "expired", "expirado"                                                 -> StatusPedido.EXPIRADO;
            default -> null;
        };
    }

    private JsonNode ler(String bruto) {
        try {
            return json.readTree(bruto);
        } catch (Exception e) {
            throw new EventoIgnoravel("Payload gravado nao e JSON valido: " + e.getMessage());
        }
    }

    private String texto(JsonNode no, String... chaves) {
        for (String chave : chaves) {
            JsonNode valor = no.get(chave);
            if (valor != null && !valor.isNull() && !valor.asText().isBlank()) {
                return valor.asText();
            }
            JsonNode dados = no.get("data");
            if (dados != null) {
                JsonNode aninhado = dados.get(chave);
                if (aninhado != null && !aninhado.isNull() && !aninhado.asText().isBlank()) {
                    return aninhado.asText();
                }
            }
        }
        return null;
    }

    /** Evento valido que nao tem nada a aplicar. Nao e falha, entao nao entra em backoff. */
    static class EventoIgnoravel extends RuntimeException {
        EventoIgnoravel(String mensagem) {
            super(mensagem);
        }
    }
}
