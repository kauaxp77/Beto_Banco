package com.betobanco.webhooks.controller;

import com.betobanco.webhooks.service.WebhookIngestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Recepcao de webhooks de pagamento.
 *
 * <p>Este endpoint faz o minimo possivel e responde rapido. Nenhuma regra de
 * negocio roda aqui: validar a assinatura, gravar o evento e sair. O
 * processamento acontece depois, no worker — assim o gateway nunca espera por
 * criacao de aluno, concessao de acesso ou envio de e-mail.
 */
@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookIngestService ingest;

    public WebhookController(WebhookIngestService ingest) {
        this.ingest = ingest;
    }

    /**
     * Recebe o corpo em bytes CRUS, e nao desserializado: a assinatura HMAC e
     * calculada sobre os bytes exatos que o provedor enviou.
     */
    @PostMapping(value = "/payment/{provider}", consumes = "*/*")
    public ResponseEntity<Map<String, String>> receber(
            @PathVariable("provider") String provider,
            @RequestBody(required = false) byte[] corpoCru,
            HttpServletRequest request) {

        Map<String, String> cabecalhos = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(
                nome -> cabecalhos.put(nome.toLowerCase(), request.getHeader(nome)));

        WebhookIngestService.Resultado resultado =
                ingest.receber(provider, corpoCru == null ? new byte[0] : corpoCru, cabecalhos);

        return switch (resultado.desfecho()) {
            case ACEITO -> ResponseEntity.ok(Map.of("status", "accepted"));
            // Duplicado tambem e 200: o provedor fez o que devia, e reenviar e
            // comportamento normal de retry. Responder erro provocaria mais
            // reenvios de um evento que ja esta guardado.
            case DUPLICADO -> ResponseEntity.ok(Map.of("status", "already_received"));
            case ASSINATURA_INVALIDA -> {
                log.warn("Webhook com assinatura invalida do provedor {}", provider);
                yield ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "invalid_signature"));
            }
            case PROVEDOR_DESCONHECIDO -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "unknown_provider"));
            case ILEGIVEL -> ResponseEntity.badRequest().body(Map.of("status", "unreadable"));
        };
    }
}
