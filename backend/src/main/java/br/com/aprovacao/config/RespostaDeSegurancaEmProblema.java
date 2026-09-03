package br.com.aprovacao.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Secao 19: "Erro no formato RFC 7807" -- para toda a API, sem excecao.
 *
 * <p>O Spring Security rejeita a requisicao dentro da cadeia de filtros, antes de
 * chegar a qualquer controlador, entao o {@code @RestControllerAdvice} nunca ve
 * essa falha: o padrao seria 403 com corpo vazio. Duas consequencias praticas
 * que este componente corrige:
 *
 * <ul>
 *   <li>Requisicao sem credencial passa a devolver <b>401</b>, nao 403. A
 *       diferenca importa para o cliente: 401 significa "renove o token e tente
 *       de novo" (o interceptor da API faz isso sozinho), enquanto 403 significa
 *       "seu perfil nao permite" e nao deve disparar renovacao nenhuma.</li>
 *   <li>O corpo passa a ser o mesmo ProblemDetail do resto da API, com o
 *       {@code type} estavel que a interface usa para decidir o que mostrar.</li>
 * </ul>
 */
@Component
public class RespostaDeSegurancaEmProblema implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String BASE_TIPO = "https://api.plataforma.com.br/problemas/";

    /** Sem credencial, ou com credencial invalida. */
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e)
            throws IOException {
        escrever(res, HttpStatus.UNAUTHORIZED, "nao-autenticado",
                "Esta rota exige autenticacao. Envie um access token valido.");
    }

    /** Autenticado, mas sem o perfil exigido (secao 20). */
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e)
            throws IOException {
        escrever(res, HttpStatus.FORBIDDEN, "acesso-negado",
                "Seu perfil nao permite esta operacao.");
    }

    private void escrever(HttpServletResponse res, HttpStatus status, String tipo, String detalhe)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("""
                {"type":"%s%s","title":"%s","status":%d,"detail":"%s","errors":[]}"""
                .formatted(BASE_TIPO, tipo, status.getReasonPhrase(), status.value(), detalhe));
    }
}
