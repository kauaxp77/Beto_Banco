package com.betobanco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica pela presenca de um access token valido.
 *
 * <p>Este filtro NUNCA rejeita: token invalido apenas nao autentica, e a
 * requisicao segue como anonima. Quem decide o que fazer com uma requisicao
 * anonima e a cadeia de autorizacao, e a resposta sai pelo
 * {@link EnvelopeAuthenticationEntryPoint}. Um filtro que respondesse 401
 * sozinho produziria resposta fora do envelope padrao da API.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIXO)) {
            jwt.validar(header.substring(PREFIXO.length())).ifPresent(usuario -> {
                List<SimpleGrantedAuthority> autoridades = usuario.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                // O principal e o proprio AuthenticatedUser: e ele que chega
                // aos controllers por @AuthenticationPrincipal.
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, autoridades);
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
