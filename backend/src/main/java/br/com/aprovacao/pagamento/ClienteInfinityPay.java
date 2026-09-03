package br.com.aprovacao.pagamento;

import br.com.aprovacao.comercial.Pedido;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.config.PropriedadesPlataforma;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Secao 12 -- "Gateway oficial: InfinityPay. Prioridade zero do projeto."
 * Secao 21 -- "Dado de cartao nunca trafega nem e armazenado por nos": pedimos ao
 * gateway um link e redirecionamos o aluno; o cartao e digitado no ambiente dele.
 *
 * <p>Secao 30 lista a dependencia de fornecedor unico como risco MEDIO e manda
 * homologar um segundo gateway a partir da Fase 2. A interface deste cliente e
 * estreita de proposito -- criar checkout e consultar pedido -- para que o segundo
 * gateway seja uma implementacao a mais, nao uma reescrita do modulo comercial.
 */
@Component
public class ClienteInfinityPay {

    private static final Logger log = LoggerFactory.getLogger(ClienteInfinityPay.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final PropriedadesPlataforma props;
    private final ObjectMapper json;

    public ClienteInfinityPay(PropriedadesPlataforma props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    public String criarCheckout(Pedido pedido) {
        String token = props.pagamento().apiToken();
        if (token == null || token.isBlank()) {
            // Sem credencial nao ha checkout. Falhar aqui e melhor do que gravar um
            // pedido com link vazio e descobrir na tela do aluno.
            throw new ProblemaNegocio(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "gateway-nao-configurado",
                    "Pagamento indisponivel no momento. Tente novamente em instantes.");
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("order_id", pedido.getId().toString());
        corpo.put("amount", pedido.valorLiquidoCentavos());
        corpo.put("currency", "BRL");
        corpo.put("customer", Map.of(
                "name", pedido.getNome() == null ? "" : pedido.getNome(),
                "email", pedido.getEmail(),
                "tax_id", pedido.getCpf() == null ? "" : pedido.getCpf()));
        // Secao 03: parcelamento em ate 12x no cartao; Pix a vista com 10% de desconto.
        corpo.put("max_installments", 12);
        corpo.put("payment_methods", new String[] {"credit_card", "pix"});

        try {
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(props.pagamento().apiUrl() + "/v1/checkouts"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", pedido.getId().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                    .build();

            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= 300) {
                log.error("InfinityPay recusou o checkout do pedido {}: HTTP {} {}",
                        pedido.getId(), resposta.statusCode(), resposta.body());
                throw new ProblemaNegocio(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "gateway-indisponivel", "Nao foi possivel abrir o pagamento. Tente novamente.");
            }
            JsonNode corpoResposta = json.readTree(resposta.body());
            JsonNode url = corpoResposta.get("checkout_url");
            if (url == null || url.isNull()) {
                url = corpoResposta.path("data").get("checkout_url");
            }
            if (url == null || url.isNull()) {
                throw new IllegalStateException("Resposta do gateway sem checkout_url: " + resposta.body());
            }
            return url.asText();

        } catch (ProblemaNegocio e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProblemaNegocio(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "gateway-indisponivel", "Nao foi possivel abrir o pagamento. Tente novamente.");
        } catch (Exception e) {
            log.error("Falha ao criar checkout do pedido {}", pedido.getId(), e);
            throw new ProblemaNegocio(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "gateway-indisponivel", "Nao foi possivel abrir o pagamento. Tente novamente.");
        }
    }
}
