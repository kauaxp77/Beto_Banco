package br.com.aprovacao.pagamento;

import br.com.aprovacao.config.PropriedadesPlataforma;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secao 12 -- POST /webhooks/infinitypay.
 *
 * <p>Este endpoint faz quatro coisas e nada alem disso: confere o HMAC, grava o
 * evento, responde 200 e acorda a fila. Liberar acesso aqui dentro violaria a
 * regra "responder 200 em ate 5s e processar em fila; nunca liberar acesso dentro
 * do request HTTP" -- e um gateway que nao recebe o 200 a tempo reenvia o evento,
 * o que multiplicaria o trabalho justamente quando ele ja esta lento.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Recebimento de eventos do gateway de pagamento (secao 12)")
public class ControladorWebhookInfinityPay {

    private static final Logger log = LoggerFactory.getLogger(ControladorWebhookInfinityPay.class);

    private final WebhookEventoRepository eventos;
    private final ProcessadorWebhook processador;
    private final PropriedadesPlataforma props;
    private final ObjectMapper json;

    public ControladorWebhookInfinityPay(WebhookEventoRepository eventos,
                                         ProcessadorWebhook processador,
                                         PropriedadesPlataforma props,
                                         ObjectMapper json) {
        this.eventos = eventos;
        this.processador = processador;
        this.props = props;
        this.json = json;
    }

    @PostMapping("/infinitypay")
    @SecurityRequirements
    @Operation(summary = "Recebe evento do gateway. HMAC + idempotencia; processamento assincrono.")
    public ResponseEntity<Resposta> receber(
            @RequestBody byte[] corpoBruto,
            @RequestHeader(name = "X-Signature", required = false) String assinatura) {

        String segredo = props.pagamento().webhookSegredo();
        if (segredo == null || segredo.isBlank()) {
            // Sem segredo configurado nao ha como distinguir o gateway de um
            // atacante. Recusar e a unica resposta segura -- aceitar "so por
            // enquanto" e como o acesso sem pagamento acontece.
            log.error("INFINITYPAY_WEBHOOK_SECRET nao configurado. Evento recusado.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Resposta("gateway-nao-configurado"));
        }

        if (!AssinaturaHmac.confere(corpoBruto, assinatura, segredo)) {
            log.warn("Webhook com assinatura invalida recusado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Resposta("assinatura-invalida"));
        }

        JsonNode payload;
        try {
            payload = json.readTree(new String(corpoBruto, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Webhook com corpo ilegivel recusado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new Resposta("payload-ilegivel"));
        }

        String eventoId = texto(payload, "event_id", "id");
        if (eventoId == null) {
            return ResponseEntity.badRequest().body(new Resposta("event_id-ausente"));
        }
        String gateway = props.pagamento().gateway();

        // Idempotencia: o indice unico (gateway, evento_id) e a garantia real; esta
        // consulta so evita o custo de uma violacao de constraint no caso comum.
        var existente = eventos.findByGatewayAndEventoId(gateway, eventoId);
        if (existente.isPresent()) {
            return ResponseEntity.ok(new Resposta("evento-ja-recebido"));
        }

        WebhookEvento evento = new WebhookEvento(
                gateway,
                eventoId,
                texto(payload, "event", "type", "status"),
                payload.toString(),
                true,
                instante(texto(payload, "occurred_at", "created_at", "timestamp")));

        try {
            eventos.saveAndFlush(evento);
        } catch (DataIntegrityViolationException corrida) {
            // Duas entregas do mesmo evento em paralelo: o indice unico decidiu.
            return ResponseEntity.ok(new Resposta("evento-ja-recebido"));
        }

        processador.agendar(evento.getId());
        return ResponseEntity.ok(new Resposta("recebido"));
    }

    private String texto(JsonNode no, String... chaves) {
        for (String chave : chaves) {
            JsonNode valor = no.get(chave);
            if (valor != null && !valor.isNull() && !valor.asText().isBlank()) {
                return valor.asText();
            }
        }
        return null;
    }

    /** Sem data no payload o evento entra com ocorrido_em nulo e a fila o trata primeiro. */
    private Instant instante(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(texto));
            } catch (NumberFormatException ignorado) {
                return null;
            }
        }
    }

    public record Resposta(String resultado) {}
}
