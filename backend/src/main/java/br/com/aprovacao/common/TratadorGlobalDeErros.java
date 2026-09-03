package br.com.aprovacao.common;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Secao 19: "Erro no formato RFC 7807: { type, title, status, detail, errors[] }".
 *
 * <p>Uma unica origem para todo corpo de erro da API. Nada de mensagem de excecao
 * vazando para o cliente: 500 responde texto generico e o detalhe fica no log com
 * o trace_id (secao 23).
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);
    private static final String BASE_TIPO = "https://api.plataforma.com.br/problemas/";

    @ExceptionHandler(ProblemaNegocio.class)
    public ProblemDetail negocio(ProblemaNegocio e) {
        return problema(e.status(), e.tipo(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "acesso-negado", "Seu perfil nao permite esta operacao.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail naoAutenticado(AuthenticationException e) {
        return problema(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Credenciais ausentes ou invalidas.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        log.error("Falha nao tratada", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno",
                "Nao foi possivel concluir a operacao. A falha ja foi registrada.");
    }

    /** Erro de validacao de payload: preenche errors[] campo a campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail corpo = problema(HttpStatus.BAD_REQUEST, "payload-invalido",
                "Um ou mais campos nao passaram na validacao.");
        List<ErroDeCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroDeCampo(f.getField(), f.getDefaultMessage()))
                .toList();
        corpo.setProperty("errors", erros);
        return ResponseEntity.badRequest().body(corpo);
    }

    private ProblemDetail problema(HttpStatus status, String tipo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatus(status);
        p.setType(URI.create(BASE_TIPO + tipo));
        p.setTitle(status.getReasonPhrase());
        p.setDetail(detalhe);
        p.setProperty("errors", List.of());
        return p;
    }

    public record ErroDeCampo(String campo, String mensagem) {}
}
