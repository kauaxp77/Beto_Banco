package com.betobanco.payments.gateway;

import com.betobanco.payments.api.PaymentGateway;
import com.betobanco.payments.api.PaymentNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Gateway oficial da plataforma.
 *
 * <p>Documento Mestre V4.0, secao 12: "Gateway oficial: InfinityPay. Prioridade
 * zero do projeto." A secao 31 registra a escolha como decisao permanente, que
 * so muda por nova versao MAJOR do documento.
 *
 * <p>Nada aqui conhece regra de negocio: a classe traduz o formato do provedor
 * para {@link PaymentNotification} e verifica a assinatura. Quem decide o que
 * fazer com um estorno e o dominio, do outro lado da porta.
 *
 * <p>Secao 21: "Dado de cartao nunca trafega nem e armazenado por nos — o
 * checkout redireciona para o ambiente da InfinityPay." Por isso este gateway
 * so recebe eventos; nao ha caminho por onde um numero de cartao entre.
 */
@Component
public class InfinityPayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(InfinityPayGateway.class);

    public static final String PROVIDER = "infinitypay";

    /**
     * Provedores costumam variar o nome do cabecalho entre a documentacao e o
     * painel. Aceitar os tres evita uma falha de integracao que so aparece com
     * o primeiro webhook real de producao.
     */
    private static final List<String> CABECALHOS_ASSINATURA =
            List.of("x-infinitypay-signature", "x-signature", "signature");

    private final ObjectMapper mapper;
    private final String segredo;

    public InfinityPayGateway(
            ObjectMapper mapper,
            @Value("${betobanco.payments.infinitypay-webhook-secret:}") String segredo) {
        this.mapper = mapper;
        this.segredo = segredo;

        if (segredo == null || segredo.isBlank()) {
            // Sem valor padrao, de proposito. Um segredo "de teste" embutido aqui
            // seria publico no repositorio e aceitaria webhook forjado de quem
            // descobrisse a URL — que e exatamente o "acesso sem pagamento" que a
            // secao 12 existe para impedir. Sem segredo, tudo e recusado.
            log.error("INFINITYPAY_WEBHOOK_SECRET nao configurado. "
                    + "Todo webhook da InfinityPay sera recusado ate que ele exista.");
        }
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean assinaturaValida(byte[] corpoCru, Map<String, String> cabecalhos) {
        if (segredo == null || segredo.isBlank() || corpoCru == null) {
            return false;
        }
        String recebida = CABECALHOS_ASSINATURA.stream()
                .map(cabecalhos::get)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
        if (recebida == null) {
            return false;
        }

        byte[] esperado = hmac(corpoCru);
        byte[] informado = decodificar(recebida);

        // MessageDigest.isEqual compara em tempo constante. Um equals() comum
        // vazaria, pelo tempo de resposta, quantos bytes iniciais ja batem.
        return informado != null && MessageDigest.isEqual(esperado, informado);
    }

    /** Auxiliar para testes e para gerar um webhook de exemplo em homologacao. */
    public String assinar(byte[] corpoCru) {
        return HexFormat.of().formatHex(hmac(corpoCru));
    }

    private byte[] hmac(byte[] corpoCru) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(corpoCru);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 indisponivel", e);
        }
    }

    /** Aceita hex e base64, com ou sem o prefixo "sha256=" que alguns painels usam. */
    private byte[] decodificar(String assinatura) {
        String limpa = assinatura.startsWith("sha256=") ? assinatura.substring(7) : assinatura;
        try {
            return HexFormat.of().parseHex(limpa.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException naoEhHex) {
            try {
                return Base64.getDecoder().decode(limpa);
            } catch (IllegalArgumentException naoEhBase64) {
                return null;
            }
        }
    }

    @Override
    public Optional<PaymentNotification> interpretar(byte[] corpoCru) {
        try {
            JsonNode raiz = mapper.readTree(corpoCru);
            // A InfinityPay aninha os dados em "data"; alguns eventos vem planos.
            JsonNode dados = raiz.hasNonNull("data") ? raiz.get("data") : raiz;

            String eventId = primeiro(raiz, dados, "event_id", "id");
            String eventType = primeiro(raiz, dados, "event", "type", "status");
            if (eventId == null || eventType == null) {
                return Optional.empty();
            }

            return Optional.of(new PaymentNotification(
                    eventId,
                    eventType,
                    tipoDe(eventType),
                    primeiro(raiz, dados, "transaction_id", "charge_id", "order_nsu", "id"),
                    primeiro(raiz, dados, "sku", "reference", "external_reference"),
                    primeiro(raiz, dados, "buyer_email", "customer_email", "email"),
                    primeiro(raiz, dados, "buyer_name", "customer_name", "name"),
                    centavos(dados),
                    dados.hasNonNull("currency") ? dados.get("currency").asText() : "BRL",
                    splits(dados),
                    momento(primeiro(raiz, dados, "occurred_at", "created_at", "timestamp"))));
        } catch (Exception e) {
            log.warn("Payload da InfinityPay ilegivel: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Secao 18: "Dinheiro em centavos, tipo inteiro. Nunca float."
     *
     * <p>Quando o provedor manda o valor em reais decimais, a conversao passa por
     * BigDecimal. {@code (long)(29.90 * 100)} devolve 2989 em ponto flutuante —
     * um centavo a menos, todo mes, em toda venda.
     */
    private long centavos(JsonNode dados) {
        if (dados.hasNonNull("amount_cents")) {
            return dados.get("amount_cents").asLong(0);
        }
        if (dados.hasNonNull("amount")) {
            return new BigDecimal(dados.get("amount").asText())
                    .movePointRight(2)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .longValueExact();
        }
        return 0L;
    }

    private List<PaymentNotification.Split> splits(JsonNode dados) {
        List<PaymentNotification.Split> splits = new ArrayList<>();
        JsonNode arr = dados.get("splits");
        if (arr != null && arr.isArray()) {
            for (JsonNode s : arr) {
                splits.add(new PaymentNotification.Split(
                        s.hasNonNull("recipient") ? s.get("recipient").asText() : null,
                        s.path("amount_cents").asLong(0),
                        s.hasNonNull("percentage") ? new BigDecimal(s.get("percentage").asText()) : null));
            }
        }
        return splits;
    }

    /** ISO 8601 ou epoch em segundos. Sem data, devolve null e a fila trata primeiro. */
    private Instant momento(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException naoEhIso) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(texto));
            } catch (NumberFormatException ignorado) {
                return null;
            }
        }
    }

    private static String primeiro(JsonNode raiz, JsonNode dados, String... campos) {
        for (String campo : campos) {
            for (JsonNode no : new JsonNode[] {dados, raiz}) {
                if (no != null && no.hasNonNull(campo)) {
                    String valor = no.get(campo).asText();
                    if (!valor.isBlank()) {
                        return valor;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Vocabulario da InfinityPay para os estados que o dominio conhece.
     *
     * <p>Status desconhecido vira IGNORADO, nunca APROVADO. Um evento novo do
     * lado do provedor fica registrado e visivel no admin, em vez de liberar
     * acesso por engano — errar para o lado de nao conceder e recuperavel; errar
     * para o lado de conceder de graca, nao.
     */
    private static PaymentNotification.Tipo tipoDe(String eventType) {
        return switch (eventType.toLowerCase(Locale.ROOT)) {
            case "paid", "approved", "aprovado",
                 "payment.approved", "charge.paid", "transaction.approved" -> PaymentNotification.Tipo.APROVADO;
            case "pending", "pendente", "waiting_payment",
                 "payment.pending", "charge.pending" -> PaymentNotification.Tipo.PENDENTE;
            case "canceled", "cancelled", "cancelado",
                 "payment.cancelled", "charge.canceled" -> PaymentNotification.Tipo.CANCELADO;
            case "refunded", "estornado",
                 "payment.refunded", "charge.refunded" -> PaymentNotification.Tipo.REEMBOLSADO;
            case "chargeback", "dispute",
                 "payment.chargeback", "charge.chargeback" -> PaymentNotification.Tipo.CHARGEBACK;
            default -> PaymentNotification.Tipo.IGNORADO;
        };
    }
}
