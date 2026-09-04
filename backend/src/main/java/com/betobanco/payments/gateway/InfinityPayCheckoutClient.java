package com.betobanco.payments.gateway;

import com.betobanco.payments.api.CheckoutGateway;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente do Checkout Integrado da InfinitePay.
 *
 * <p>ENDERECOS NOVOS. A InfinitePay migrou o checkout e os antigos vao parar de
 * responder:
 *
 * <pre>
 * antigo  POST https://api.infinitepay.io/invoices/public/checkout/links
 * novo    POST https://api.checkout.infinitepay.io/links
 *
 * antigo  POST https://api.infinitepay.io/invoices/public/checkout/payment_check
 * novo    POST https://api.checkout.infinitepay.io/payment_check
 * </pre>
 *
 * <p>A base fica em configuracao ({@code betobanco.payments.infinitypay.base-url})
 * para que a proxima migracao de endereco seja uma variavel de ambiente, e nao
 * um deploy.
 *
 * <p>ATENCAO: o contrato abaixo foi escrito a partir da documentacao publica do
 * Checkout Integrado. Antes de vender de verdade, uma chamada de homologacao
 * precisa confirmar os nomes dos campos — principalmente o que a criacao de
 * link devolve alem de {@code url}.
 */
@Component
public class InfinityPayCheckoutClient implements CheckoutGateway {

    private static final Logger log = LoggerFactory.getLogger(InfinityPayCheckoutClient.class);

    private final RestClient http;
    private final String handle;
    private final String urlDeRetorno;
    private final String urlDeWebhook;

    public InfinityPayCheckoutClient(
            RestClient.Builder builder,
            @Value("${betobanco.payments.infinitypay.base-url:https://api.checkout.infinitepay.io}")
            String baseUrl,
            @Value("${betobanco.payments.infinitypay.handle:}") String handle,
            @Value("${betobanco.payments.infinitypay.redirect-url:}") String urlDeRetorno,
            @Value("${betobanco.payments.infinitypay.webhook-url:}") String urlDeWebhook) {

        // Tempo limite curto e explicito: o comprador esta parado numa tela
        // esperando o checkout abrir. Sem isso o padrao e nao ter limite, e uma
        // instabilidade do provedor viraria requisicao pendurada ate o timeout
        // do servidor — com o comprador olhando para um botao travado.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));

        this.http = builder.baseUrl(baseUrl).requestFactory(fabrica).build();
        this.handle = handle;
        this.urlDeRetorno = urlDeRetorno;
        this.urlDeWebhook = urlDeWebhook;

        if (handle == null || handle.isBlank()) {
            // Sem InfiniteTag nao ha para qual conta cobrar. Avisa no boot em
            // vez de falhar na primeira venda.
            log.error("betobanco.payments.infinitypay.handle nao configurado: "
                    + "o checkout vai recusar toda tentativa de compra.");
        }
    }

    @Override
    public LinkDeCheckout criarLink(PedidoDeCheckout pedido) {
        exigirHandle();

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("handle", handle);
        corpo.put("order_nsu", pedido.orderNsu());
        // "O valor do produto deve ser colocado em centavos" — mesma unidade
        // que a secao 18 exige internamente, entao nao ha conversao no meio.
        corpo.put("items", List.of(Map.of(
                "quantity", 1,
                "price", pedido.valorCentavos(),
                "description", pedido.descricao())));

        if (!urlDeRetorno.isBlank()) {
            corpo.put("redirect_url", urlDeRetorno);
        }
        if (!urlDeWebhook.isBlank()) {
            corpo.put("webhook_url", urlDeWebhook);
        }
        if (pedido.email() != null) {
            Map<String, Object> comprador = new LinkedHashMap<>();
            comprador.put("name", pedido.nome());
            comprador.put("email", pedido.email());
            if (pedido.telefone() != null && !pedido.telefone().isBlank()) {
                comprador.put("phone_number", pedido.telefone());
            }
            corpo.put("customer", comprador);
        }

        Map<?, ?> resposta = postar("/links", corpo, "criar o link de pagamento");
        Object url = resposta.get("url");
        if (url == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "A InfinitePay não devolveu a URL de pagamento.");
        }

        // O slug so aparece no webhook (invoice_slug); a criacao do link nao o
        // devolve. Aceitamos se vier, para nao depender do webhook a toa.
        Object slug = resposta.get("slug");
        return new LinkDeCheckout(String.valueOf(url), slug == null ? null : String.valueOf(slug));
    }

    @Override
    public Confirmacao confirmar(String orderNsu, String transactionNsu, String invoiceSlug) {
        exigirHandle();

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("handle", handle);
        corpo.put("order_nsu", orderNsu);
        corpo.put("transaction_nsu", transactionNsu);
        corpo.put("slug", invoiceSlug);

        Map<?, ?> r = postar("/payment_check", corpo, "confirmar o pagamento");

        boolean sucesso = Boolean.TRUE.equals(r.get("success"));
        boolean pago = sucesso && Boolean.TRUE.equals(r.get("paid"));

        return new Confirmacao(pago,
                inteiro(r.get("amount")),
                inteiro(r.get("paid_amount")),
                (int) inteiro(r.get("installments")),
                r.get("capture_method") == null ? null : String.valueOf(r.get("capture_method")));
    }

    // ------------------------------------------------------------------

    private Map<?, ?> postar(String caminho, Map<String, Object> corpo, String oQue) {
        try {
            Map<?, ?> resposta = http.post()
                    .uri(caminho)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(Map.class);

            if (resposta == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "A InfinitePay respondeu vazio ao " + oQue + ".");
            }
            return resposta;
        } catch (RestClientException e) {
            // A mensagem do provedor nao vai para o comprador: ela pode conter
            // detalhe de configuracao da conta. Vai para o log.
            log.error("Falha ao {} na InfinitePay ({}).", oQue, caminho, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Não foi possível " + oQue + " agora. Tente novamente em instantes.");
        }
    }

    private void exigirHandle() {
        if (handle == null || handle.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Checkout indisponível: a InfiniteTag da conta não foi configurada.");
        }
    }

    /** Valores monetarios chegam como inteiro em centavos; ausentes viram zero. */
    private static long inteiro(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        if (valor == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(valor));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
