package br.com.aprovacao.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Secao 20 -- regras de sessao.
 *
 * <p>Uma linha por refresh token emitido. A familia liga todos os tokens que
 * descendem de um mesmo login: quando um refresh ja rotacionado reaparece, o mais
 * provavel e que ele foi roubado, e a defesa e derrubar a familia inteira -- o
 * atacante perde o acesso e o dono legitimo apenas refaz o login.
 */
@Entity
@Table(name = "sessao")
public class Sessao {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(name = "refresh_token_hash", nullable = false, unique = true)
    private String refreshTokenHash;

    private String dispositivo;

    @Column(name = "user_agent")
    private String userAgent;

    private String ip;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "motivo_revogacao")
    private String motivoRevogacao;

    protected Sessao() {}

    public Sessao(UUID usuarioId, UUID familiaId, String refreshTokenHash,
                  String dispositivo, String userAgent, String ip, Instant expiraEm) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.refreshTokenHash = refreshTokenHash;
        this.dispositivo = dispositivo;
        this.userAgent = userAgent;
        this.ip = ip;
        this.expiraEm = expiraEm;
    }

    public boolean estaViva() {
        return revogadoEm == null && expiraEm.isAfter(Instant.now());
    }

    public void revogar(String motivo) {
        if (revogadoEm == null) {
            this.revogadoEm = Instant.now();
            this.motivoRevogacao = motivo;
        }
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getFamiliaId() { return familiaId; }
    public String getDispositivo() { return dispositivo; }
    public String getUserAgent() { return userAgent; }
    public String getIp() { return ip; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getRevogadoEm() { return revogadoEm; }
    public String getMotivoRevogacao() { return motivoRevogacao; }
}
