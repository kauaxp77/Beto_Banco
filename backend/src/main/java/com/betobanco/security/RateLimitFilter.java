package com.betobanco.security;

import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.ErrorPayload;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas nos endpoints de autenticacao, por IP e por rota.
 *
 * <p>LIMITACAO CONHECIDA: o contador vive em memoria, entao o limite efetivo
 * multiplica pelo numero de instancias. Uma implantacao replicada exige mover
 * isto para Redis. Registrado na secao 6.6 da spec.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration JANELA = Duration.ofMinutes(1);

    private static final Set<String> PROTEGIDOS =
            Set.of("/auth/login", "/auth/forgot-password", "/auth/reset-password");

    private final Map<String, Bucket> baldes = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final int limite;

    public RateLimitFilter(
            ObjectMapper mapper,
            @org.springframework.beans.factory.annotation.Value(
                    "${betobanco.auth.rate-limit-per-minute:30}") int limite) {
        this.mapper = mapper;
        this.limite = limite;
    }

    public int limite() {
        return limite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Casamento por sufixo da URI, e nao por getServletPath(): sob o
        // context-path /api/v1 a URI e "/api/v1/auth/login", e em teste com
        // MockMvc e "/auth/login". O sufixo cobre os dois.
        String uri = request.getRequestURI();
        String rota = PROTEGIDOS.stream().filter(uri::endsWith).findFirst().orElse(null);

        if (rota == null) {
            chain.doFilter(request, response);
            return;
        }

        Bucket balde = baldes.computeIfAbsent(
                rota + "|" + request.getRemoteAddr(),
                k -> Bucket.builder()
                        .addLimit(Bandwidth.builder().capacity(limite)
                                .refillGreedy(limite, JANELA).build())
                        .build());

        if (balde.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        ErrorPayload payload = new ErrorPayload(
                ErrorCode.RATE_LIMIT_EXCEEDED.name(), "Limite de requisições excedido",
                ErrorCode.RATE_LIMIT_EXCEEDED.httpStatus(), request.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now().toString(), List.of());

        response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(payload));
    }
}
