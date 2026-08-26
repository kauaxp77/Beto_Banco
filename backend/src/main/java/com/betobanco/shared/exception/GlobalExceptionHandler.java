package com.betobanco.shared.exception;

import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> negocio(BusinessException ex, HttpServletRequest req) {
        return montar(ex.code(), ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validacao(MethodArgumentNotValidException ex,
                                                      HttpServletRequest req) {
        List<FieldErrorItem> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorItem(e.getField(), e.getDefaultMessage()))
                .toList();
        return montar(ErrorCode.VALIDATION_ERROR, "Dados inválidos", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> ilegivel(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        return montar(ErrorCode.MALFORMED_REQUEST, "Corpo da requisição inválido", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> negado(AccessDeniedException ex,
                                                   HttpServletRequest req) {
        return montar(ErrorCode.FORBIDDEN, "Acesso negado", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> inesperada(Exception ex, HttpServletRequest req) {
        log.error("Erro nao tratado em {}", req.getRequestURI(), ex);
        return montar(ErrorCode.INTERNAL_ERROR, "Erro interno do servidor", req, List.of());
    }

    private ResponseEntity<ApiResponse<Void>> montar(ErrorCode code, String message,
                                                     HttpServletRequest req,
                                                     List<FieldErrorItem> campos) {
        ErrorPayload payload = new ErrorPayload(
                code.name(),
                message,
                code.httpStatus(),
                req.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY),
                Instant.now().toString(),
                campos);
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.error(payload));
    }
}
