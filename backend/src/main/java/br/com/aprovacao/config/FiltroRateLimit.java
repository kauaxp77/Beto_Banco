package br.com.aprovacao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Secao 19: "Limite de requisicao: 60/min por IP no publico, 10/min em login e
 * recuperacao de senha."
 *
 * <p>Janela fixa por minuto, em memoria. Serve ate a API rodar em uma instancia so;
 * a partir de duas o contador precisa migrar para o Redis ja provisionado na secao
 * 23, senao o limite efetivo vira N vezes o configurado. O TODO esta marcado no
 * ponto exato da troca.
 */
@Component
public class FiltroRateLimit extends OncePerRequestFilter {

    private final PropriedadesPlataforma props;
    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();

    public FiltroRateLimit(PropriedadesPlataforma props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String caminho = req.getRequestURI();
        // Webhook do gateway nao pode ser limitado por IP: uma rajada de retentativa
        // legitima viraria acesso nao liberado (secao 30, risco CRITICO-FINANCEIRO).
        if (caminho.startsWith("/api/v1/webhooks/")) {
            chain.doFilter(req, res);
            return;
        }

        int teto = ehRotaSensivel(caminho)
                ? props.rateLimit().autenticacaoPorMinuto()
                : props.rateLimit().publicoPorMinuto();

        String chave = ipDoCliente(req) + "|" + (ehRotaSensivel(caminho) ? "auth" : "publico");
        // TODO(fase-2): trocar por INCR + EXPIRE no Redis quando houver mais de uma replica.
        Contador contador = contadores.compute(chave, (k, atual) ->
                atual == null || atual.expirou() ? new Contador() : atual);

        if (contador.usos.incrementAndGet() > teto) {
            long esperaSegundos = contador.segundosRestantes();
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", String.valueOf(esperaSegundos));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            res.getWriter().write("""
                    {"type":"https://api.plataforma.com.br/problemas/limite-excedido",\
                    "title":"Too Many Requests","status":429,\
                    "detail":"Muitas requisicoes. Tente novamente em %d segundos.","errors":[]}"""
                    .formatted(esperaSegundos));
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean ehRotaSensivel(String caminho) {
        return caminho.startsWith("/api/v1/auth/");
    }

    /**
     * Atras da Vercel/Railway o IP real vem em X-Forwarded-For. Usamos o primeiro
     * elemento, que e o cliente; os seguintes sao os proxies.
     */
    private String ipDoCliente(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static final class Contador {
        private final Instant inicio = Instant.now();
        private final AtomicInteger usos = new AtomicInteger();

        boolean expirou() {
            return Duration.between(inicio, Instant.now()).toSeconds() >= 60;
        }

        long segundosRestantes() {
            return Math.max(1, 60 - Duration.between(inicio, Instant.now()).toSeconds());
        }
    }
}
