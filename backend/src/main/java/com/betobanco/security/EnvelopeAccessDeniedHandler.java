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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Faz o 403 vindo da cadeia de filtros sair no mesmo envelope do resto da API. */
@Component
public class EnvelopeAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    public EnvelopeAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        ErrorPayload payload = new ErrorPayload(
                ErrorCode.FORBIDDEN.name(), "Acesso negado",
                ErrorCode.FORBIDDEN.httpStatus(), request.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now().toString(), List.of());

        response.setStatus(ErrorCode.FORBIDDEN.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(payload));
    }
}
