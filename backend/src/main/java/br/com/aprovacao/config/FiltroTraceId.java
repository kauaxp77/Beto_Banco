package br.com.aprovacao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Secao 23: "Logs estruturados em JSON com trace_id percorrendo a requisicao."
 *
 * <p>Reaproveita o trace de um proxy a frente quando existe, para que o mesmo id
 * ligue frontend, API e Sentry. Roda antes de tudo -- inclusive antes do filtro de
 * rate limit -- para que ate a requisicao rejeitada apareca no log com id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroTraceId extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Trace-Id";
    public static final String CHAVE_MDC = "trace_id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String trace = req.getHeader(CABECALHO);
        if (trace == null || trace.isBlank() || trace.length() > 64) {
            trace = UUID.randomUUID().toString();
        }
        MDC.put(CHAVE_MDC, trace);
        res.setHeader(CABECALHO, trace);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }
}
