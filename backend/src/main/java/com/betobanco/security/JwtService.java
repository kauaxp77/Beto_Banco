package com.betobanco.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    private final SecretKey chave;
    private final Duration duracao;

    public JwtService(@Value("${betobanco.auth.jwt-secret}") String segredo,
                      @Value("${betobanco.auth.access-token-minutes}") long minutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.duracao = Duration.ofMinutes(minutos);
    }

    public String gerar(UUID id, String email, Set<String> roles) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(id.toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(duracao)))
                .signWith(chave)
                .compact();
    }

    public Optional<AuthenticatedUser> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<?> brutas = claims.get(CLAIM_ROLES, List.class);
            Set<String> roles = brutas == null
                    ? Set.of()
                    : brutas.stream().map(String::valueOf)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    roles));
        } catch (Exception e) {
            // Token invalido nao e erro do servidor: nao polui o log em ERROR.
            log.debug("Token recusado: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public long duracaoSegundos() {
        return duracao.toSeconds();
    }
}
