package br.com.aprovacao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Secao 27 -- resolve o tenant da requisicao e o deixa disponivel para a camada de
 * dados, que o repassa ao PostgreSQL como app.tenant_id (politica de RLS da V3).
 *
 * <p>Precedencia: token autenticado, depois dominio do Host, depois tenant padrao.
 * O token vem primeiro de proposito -- um usuario nunca deve conseguir ler dados de
 * outro tenant so trocando o cabecalho Host.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class FiltroTenant extends OncePerRequestFilter {

    private static final ThreadLocal<UUID> ATUAL = new ThreadLocal<>();

    private final PropriedadesPlataforma props;
    private final ResolvedorDeTenant resolvedor;

    public FiltroTenant(PropriedadesPlataforma props, ResolvedorDeTenant resolvedor) {
        this.props = props;
        this.resolvedor = resolvedor;
    }

    /** Tenant da requisicao corrente. Fora de uma requisicao, o tenant padrao. */
    public static UUID atual(UUID padrao) {
        UUID t = ATUAL.get();
        return t != null ? t : padrao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            ATUAL.set(resolver(req));
            chain.doFilter(req, res);
        } finally {
            // Sem o remove a thread do pool levaria o tenant para a proxima
            // requisicao -- vazamento entre clientes, nao apenas um bug de dado.
            ATUAL.remove();
        }
    }

    private UUID resolver(HttpServletRequest req) {
        UsuarioAutenticado autenticado = UsuarioAutenticado.atual();
        if (autenticado != null) {
            return autenticado.tenantId();
        }
        String host = req.getHeader("Host");
        if (host != null && !host.isBlank()) {
            UUID porDominio = resolvedor.porDominio(host.split(":")[0]);
            if (porDominio != null) {
                return porDominio;
            }
        }
        return props.tenantPadrao();
    }
}
