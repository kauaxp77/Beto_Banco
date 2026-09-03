package br.com.aprovacao.common;

import org.springframework.http.HttpStatus;

/**
 * Falha de negocio que vira uma resposta RFC 7807 (secao 19).
 *
 * <p>O tipo e a chave estavel que o frontend usa para decidir o que mostrar; o
 * detalhe e texto para humano e pode mudar sem quebrar cliente.
 */
public class ProblemaNegocio extends RuntimeException {

    private final String tipo;
    private final HttpStatus status;

    public ProblemaNegocio(HttpStatus status, String tipo, String detalhe) {
        super(detalhe);
        this.status = status;
        this.tipo = tipo;
    }

    public static ProblemaNegocio naoEncontrado(String recurso) {
        return new ProblemaNegocio(HttpStatus.NOT_FOUND, "recurso-nao-encontrado", recurso + " nao encontrado.");
    }

    public static ProblemaNegocio conflito(String tipo, String detalhe) {
        return new ProblemaNegocio(HttpStatus.CONFLICT, tipo, detalhe);
    }

    public static ProblemaNegocio invalido(String tipo, String detalhe) {
        return new ProblemaNegocio(HttpStatus.UNPROCESSABLE_ENTITY, tipo, detalhe);
    }

    public static ProblemaNegocio proibido(String detalhe) {
        return new ProblemaNegocio(HttpStatus.FORBIDDEN, "acesso-negado", detalhe);
    }

    public String tipo() {
        return tipo;
    }

    public HttpStatus status() {
        return status;
    }
}
