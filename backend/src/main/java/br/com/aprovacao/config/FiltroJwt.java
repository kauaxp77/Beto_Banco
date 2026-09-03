package br.com.aprovacao.config;

import br.com.aprovacao.auth.Perfil;
import br.com.aprovacao.auth.ServicoJwt;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Le o access token do cabecalho Authorization e popula o contexto de seguranca.
 *
 * <p>Token ausente ou invalido nao levanta erro aqui: o filtro apenas nao autentica
 * e o Spring Security decide se a rota exigia autenticacao. Isso mantem as rotas
 * publicas funcionando mesmo quando o navegador manda um token velho.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private final ServicoJwt jwt;

    public FiltroJwt(ServicoJwt jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String cabecalho = req.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            Claims claims = jwt.lerAccessToken(cabecalho.substring(7));
            if (claims != null) {
                SecurityContextHolder.getContext().setAuthentication(autenticacao(claims));
            }
        }
        chain.doFilter(req, res);
    }

    private UsernamePasswordAuthenticationToken autenticacao(Claims claims) {
        Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);
        Object bruto = claims.get("perfis");
        if (bruto instanceof List<?> lista) {
            for (Object item : lista) {
                try {
                    perfis.add(Perfil.valueOf(String.valueOf(item)));
                } catch (IllegalArgumentException ignorado) {
                    // Perfil removido do enum depois de o token ter sido emitido:
                    // ignorar e mais seguro do que rejeitar a requisicao inteira.
                }
            }
        }

        UsuarioAutenticado usuario = new UsuarioAutenticado(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("tenant_id", String.class)),
                UUID.fromString(claims.get("sid", String.class)),
                perfis);

        return new UsernamePasswordAuthenticationToken(usuario, null, usuario.authorities());
    }
}
