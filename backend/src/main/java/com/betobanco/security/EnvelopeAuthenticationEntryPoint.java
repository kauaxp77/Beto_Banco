package com.betobanco.security;

import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.ErrorPayload;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Faz o 401 vindo da cadeia de filtros sair no mesmo envelope do resto da API. */
@Component
public class EnvelopeAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public EnvelopeAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        ErrorPayload payload = new ErrorPayload(
                ErrorCode.UNAUTHORIZED.name(), "Não autenticado",
                ErrorCode.UNAUTHORIZED.httpStatus(), request.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now().toString(), List.of());

        response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(payload));
    }
}
