package br.com.aprovacao.auth;

import br.com.aprovacao.config.PropriedadesPlataforma;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Secao 20 -- emissao e leitura de token.
 *
 * <p>O access token e um JWT assinado e sem estado; o refresh token nao e JWT: e
 * um valor aleatorio de 256 bits guardado apenas como hash SHA-256 na tabela
 * sessao. Assim um vazamento do banco nao entrega refresh token utilizavel, e a
 * revogacao e imediata -- coisa que um JWT auto-contido nao permite.
 */
@Service
public class ServicoJwt {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKey chave;
    private final PropriedadesPlataforma props;

    public ServicoJwt(PropriedadesPlataforma props) {
        this.props = props;
        byte[] segredo = props.jwt().segredo().getBytes(StandardCharsets.UTF_8);
        if (segredo.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET precisa de pelo menos 32 bytes. Secao 21: segredo em variavel de ambiente ou cofre.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo);
    }

    public String emitirAccessToken(Usuario usuario, UUID sessaoId) {
        Instant agora = Instant.now();
        Instant expira = agora.plusSeconds(props.jwt().accessMinutos() * 60L);
        List<String> perfis = usuario.getPerfis().stream().map(Enum::name).toList();

        return Jwts.builder()
                .issuer(props.jwt().emissor())
                .subject(usuario.getId().toString())
                .claim("tenant_id", usuario.getTenantId().toString())
                .claim("perfis", perfis)
                .claim("sid", sessaoId.toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expira))
                .signWith(chave)
                .compact();
    }

    public Claims lerAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(props.jwt().emissor())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Valor opaco entregue ao cliente. Nunca persistido em claro. */
    public String gerarRefreshToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashDeRefresh(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }

    public Instant expiracaoDoRefresh() {
        return Instant.now().plusSeconds(props.jwt().refreshDias() * 86400L);
    }

    public long accessTokenSegundos() {
        return props.jwt().accessMinutos() * 60L;
    }

    public Set<String> perfisComMfaObrigatorio() {
        return Set.copyOf(props.seguranca().mfaObrigatorioPerfis());
    }
}
