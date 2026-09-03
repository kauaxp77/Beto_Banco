package br.com.aprovacao.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Secao 19: "POST /auth/senha/recuperar -- token de uso unico, 30 min." */
@Entity
@Table(name = "token_recuperacao")
public class TokenRecuperacao {

    public static final int VALIDADE_MINUTOS = 30;

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected TokenRecuperacao() {}

    public TokenRecuperacao(UUID usuarioId, String tokenHash) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEm = Instant.now().plusSeconds(VALIDADE_MINUTOS * 60L);
    }

    public boolean utilizavel() {
        return usadoEm == null && expiraEm.isAfter(Instant.now());
    }

    public void marcarUsado() {
        this.usadoEm = Instant.now();
    }

    public UUID getUsuarioId() { return usuarioId; }
}
