package com.betobanco.payments.gateway;

import com.betobanco.payments.api.PaymentGateway;
import com.betobanco.payments.api.PaymentNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gateway de referencia, com formato proprio e simples.
 *
 * <p>Existe por dois motivos. Primeiro, permite exercitar todo o fluxo de
 * webhook — idempotencia, retry, liberacao, estorno — sem depender de
 * credenciais de nenhum provedor. Segundo, serve de espelho: quando a
 * documentacao da InfinitePay chegar, a implementacao dela e outra classe
 * como esta, e nada mais do sistema muda.
 *
 * <p>A assinatura e um SHA-256 de {@code segredo + corpo}, em hex, no
 * cabecalho {@code X-Signature}.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    public static final String PROVIDER = "fake";
    public static final String CABECALHO_ASSINATURA = "x-signature";

    private final ObjectMapper mapper;
    private final String segredo;

    public FakePaymentGateway(
            ObjectMapper mapper,
            @org.springframework.beans.factory.annotation.Value(
                    "${betobanco.payments.fake-webhook-secret:segredo-de-teste}") String segredo) {
        this.mapper = mapper;
        this.segredo = segredo;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean assinaturaValida(byte[] corpoCru, Map<String, String> cabecalhos) {
        String recebida = cabecalhos.get(CABECALHO_ASSINATURA);
        if (recebida == null || recebida.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                assinar(corpoCru).getBytes(StandardCharsets.UTF_8),
                recebida.getBytes(StandardCharsets.UTF_8));
    }

    /** Auxiliar para testes e para quem for gerar um webhook de exemplo. */
    public String assinar(byte[] corpoCru) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(segredo.getBytes(StandardCharsets.UTF_8));
            digest.update(corpoCru);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    @Override
    public Optional<PaymentNotification> interpretar(byte[] corpoCru) {
        try {
            JsonNode raiz = mapper.readTree(corpoCru);

            String eventId = texto(raiz, "event_id");
            String eventType = texto(raiz, "event");
            if (eventId == null || eventType == null) {
                return Optional.empty();
            }

            List<PaymentNotification.Split> splits = new ArrayList<>();
            JsonNode arr = raiz.get("splits");
            if (arr != null && arr.isArray()) {
                for (JsonNode s : arr) {
                    splits.add(new PaymentNotification.Split(
                            texto(s, "recipient"),
                            s.path("amount_cents").asLong(0),
                            s.hasNonNull("percentage")
                                    ? new BigDecimal(s.get("percentage").asText())
                                    : null));
                }
            }

            return Optional.of(new PaymentNotification(
                    eventId,
                    eventType,
                    tipoDe(eventType),
                    texto(raiz, "transaction_id"),
                    texto(raiz, "sku"),
                    texto(raiz, "buyer_email"),
                    texto(raiz, "buyer_name"),
                    raiz.path("amount_cents").asLong(0),
                    raiz.hasNonNull("currency") ? raiz.get("currency").asText() : "BRL",
                    splits));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String texto(JsonNode no, String campo) {
        return no.hasNonNull(campo) ? no.get(campo).asText() : null;
    }

    private static PaymentNotification.Tipo tipoDe(String eventType) {
        return switch (eventType) {
            case "payment.approved" -> PaymentNotification.Tipo.APROVADO;
            case "payment.pending" -> PaymentNotification.Tipo.PENDENTE;
            case "payment.cancelled" -> PaymentNotification.Tipo.CANCELADO;
            case "payment.refunded" -> PaymentNotification.Tipo.REEMBOLSADO;
            case "payment.chargeback" -> PaymentNotification.Tipo.CHARGEBACK;
            default -> PaymentNotification.Tipo.IGNORADO;
        };
    }
}
