package com.betobanco.shared.exception;

import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Traduz toda excecao para o envelope de erro padrao da API.
 *
 * <p>Estende {@link ResponseEntityExceptionHandler} porque o
 * {@code ExceptionHandlerExceptionResolver} roda antes do
 * {@code DefaultHandlerExceptionResolver}: sem isso o {@code @ExceptionHandler(Exception.class)}
 * captura as excecoes que o proprio Spring MVC lanca para sinalizar erro do cliente
 * (rota inexistente, metodo errado, parametro ausente, tipo invalido) e devolve 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MENSAGEM_INTERNA = "Erro interno do servidor";

    // ------------------------------------------------------------------
    // Excecoes proprias da aplicacao
    // ------------------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> negocio(BusinessException ex, HttpServletRequest req) {
        log.warn("Erro de negocio {} em {}: {}", ex.code(), req.getRequestURI(), ex.getMessage());
        return montar(ex.code(), ex.getMessage(), ex.code().httpStatus(), req.getRequestURI(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> negado(AccessDeniedException ex,
                                                    HttpServletRequest req) {
        log.warn("Acesso negado em {}", req.getRequestURI());
        return montar(ErrorCode.FORBIDDEN, "Acesso negado",
                ErrorCode.FORBIDDEN.httpStatus(), req.getRequestURI(), List.of());
    }

    /**
     * {@link ResponseStatusException} nao e coberta pela classe-base (que so trata a subclasse
     * {@code ErrorResponseException}), entao sem este handler ela cairia na rede de 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> statusExplicito(ResponseStatusException ex, WebRequest request) {
        return handleExceptionInternal(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /** Ultima rede: nunca vaza a mensagem original e e o unico caminho que loga em ERROR. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> inesperada(Exception ex, HttpServletRequest req) {
        log.error("Erro nao tratado em {}", req.getRequestURI(), ex);
        return montar(ErrorCode.INTERNAL_ERROR, MENSAGEM_INTERNA,
                ErrorCode.INTERNAL_ERROR.httpStatus(), req.getRequestURI(), List.of());
    }

    // ------------------------------------------------------------------
    // Sobrescritas da classe-base
    // ------------------------------------------------------------------

    /** A spec exige 422 para falha de validacao de corpo; o padrao da classe-base e 400. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorItem> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorItem(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(ErrorCode.VALIDATION_ERROR, "Dados inválidos",
                ErrorCode.VALIDATION_ERROR.httpStatus(), headers, request, campos);
    }

    /** Corpo ilegivel continua em 400 com code MALFORMED_REQUEST. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return responder(ErrorCode.MALFORMED_REQUEST, "Corpo da requisição inválido",
                ErrorCode.MALFORMED_REQUEST.httpStatus(), headers, request, List.of());
    }

    /**
     * Ponto unico por onde passam todas as excecoes tratadas pela classe-base.
     * Embrulha a resposta no envelope padrao derivando o {@link ErrorCode} do status HTTP
     * que o Spring ja determinou. 4xx e erro do cliente: loga em WARN, nunca em ERROR.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest
                && servletWebRequest.getResponse() != null
                && servletWebRequest.getResponse().isCommitted()) {
            log.warn("Resposta ja commitada, ignorando {}", ex.getClass().getSimpleName());
            return null;
        }
        if (statusCode.is5xxServerError()) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, RequestAttributes.SCOPE_REQUEST);
            log.error("Erro de servidor {} em {}", statusCode.value(), caminho(request), ex);
        } else {
            log.warn("Erro de cliente {} em {}: {}",
                    statusCode.value(), caminho(request), ex.getClass().getSimpleName());
        }
        ErrorCode code = codigoPara(statusCode);
        return responder(code, mensagemPara(statusCode), statusCode.value(), headers, request, List.of());
    }

    // ------------------------------------------------------------------
    // Montagem do envelope
    // ------------------------------------------------------------------

    private ResponseEntity<Object> responder(ErrorCode code, String message, int status,
                                             HttpHeaders headers, WebRequest request,
                                             List<FieldErrorItem> campos) {
        ApiResponse<Void> corpo = envelope(code, message, status, caminho(request), campos);
        return new ResponseEntity<>(corpo, headers == null ? new HttpHeaders() : headers, status);
    }

    private ResponseEntity<ApiResponse<Void>> montar(ErrorCode code, String message, int status,
                                                     String path, List<FieldErrorItem> campos) {
        return ResponseEntity.status(status).body(envelope(code, message, status, path, campos));
    }

    private ApiResponse<Void> envelope(ErrorCode code, String message, int status,
                                       String path, List<FieldErrorItem> campos) {
        return ApiResponse.error(new ErrorPayload(
                code.name(),
                message,
                status,
                path,
                MDC.get(TraceIdFilter.MDC_KEY),
                Instant.now().toString(),
                campos));
    }

    private String caminho(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    /** Deriva o code estavel a partir do status que o Spring MVC ja determinou. */
    private ErrorCode codigoPara(HttpStatusCode statusCode) {
        return Arrays.stream(ErrorCode.values())
                .filter(c -> c.httpStatus() == statusCode.value())
                .findFirst()
                .orElse(statusCode.is5xxServerError()
                        ? ErrorCode.INTERNAL_ERROR
                        : ErrorCode.CLIENT_ERROR);
    }

    /** Mensagem generica por status: nunca a mensagem original da excecao. */
    private String mensagemPara(HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return MENSAGEM_INTERNA;
        }
        return switch (statusCode.value()) {
            case 400 -> "Requisição inválida";
            case 401 -> "Não autenticado";
            case 403 -> "Acesso negado";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método HTTP não suportado";
            case 406 -> "Formato de resposta não suportado";
            case 409 -> "Conflito de estado";
            case 413 -> "Conteúdo maior que o permitido";
            case 415 -> "Tipo de mídia não suportado";
            case 422 -> "Dados inválidos";
            case 429 -> "Limite de requisições excedido";
            default -> {
                HttpStatus conhecido = HttpStatus.resolve(statusCode.value());
                yield conhecido != null ? conhecido.getReasonPhrase() : "Requisição inválida";
            }
        };
    }
}
