package br.com.aprovacao.config;

import br.com.aprovacao.auth.Perfil;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Identidade da requisicao corrente, montada a partir do access token.
 *
 * <p>Carrega o tenant junto: cada consulta precisa dele, e busca-lo de novo no
 * banco a cada endpoint seria um round-trip por requisicao sem ganho nenhum.
 */
public record UsuarioAutenticado(UUID id, UUID tenantId, UUID sessaoId, Set<Perfil> perfis) {

    public Collection<GrantedAuthority> authorities() {
        return perfis.stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.authority()))
                .toList();
    }

    public boolean tem(Perfil perfil) {
        return perfis.contains(perfil);
    }

    /** Null quando a rota e publica. Endpoint autenticado deve usar {@link #obrigatorio()}. */
    public static UsuarioAutenticado atual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return u;
        }
        return null;
    }

    public static UsuarioAutenticado obrigatorio() {
        UsuarioAutenticado u = atual();
        if (u == null) {
            throw new IllegalStateException(
                    "Rota autenticada sem principal no contexto. Verifique o matcher no SecurityConfig.");
        }
        return u;
    }
}
