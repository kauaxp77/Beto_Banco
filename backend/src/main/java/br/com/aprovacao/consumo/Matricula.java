package br.com.aprovacao.consumo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Secao 03: "Acesso nao e vitalicio. Todo produto avulso vale 12 meses a contar da
 * aprovacao do pagamento." Secao 31 registra isso como decisao permanente.
 */
@Entity
@Table(name = "matricula")
public class Matricula {

    public enum Status { ATIVA, EXPIRADA, REVOGADA, BLOQUEADA }

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(name = "pedido_id")
    private UUID pedidoId;

    @Column(name = "inicia_em", nullable = false)
    private Instant iniciaEm = Instant.now();

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ATIVA;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    protected Matricula() {}

    public Matricula(UUID tenantId, UUID usuarioId, UUID cursoId, UUID pedidoId, int diasAcesso) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.usuarioId = usuarioId;
        this.cursoId = cursoId;
        this.pedidoId = pedidoId;
        this.iniciaEm = Instant.now();
        this.expiraEm = iniciaEm.plus(diasAcesso, ChronoUnit.DAYS);
    }

    public boolean liberaConteudo() {
        return status == Status.ATIVA && excluidoEm == null && expiraEm.isAfter(Instant.now());
    }

    /**
     * Secao 12 -- ESTORNADO e CANCELADO revogam; CHARGEBACK revoga e bloqueia
     * recompra, que e por isso um estado distinto e nao um sinonimo de revogada.
     */
    public void revogar() {
        this.status = Status.REVOGADA;
    }

    public void bloquear() {
        this.status = Status.BLOQUEADA;
    }

    public void expirar() {
        this.status = Status.EXPIRADA;
    }

    /** Renovacao empurra a validade a partir de hoje, nunca do vencimento antigo. */
    public void renovarPor(int dias) {
        this.status = Status.ATIVA;
        this.iniciaEm = Instant.now();
        this.expiraEm = Instant.now().plus(dias, ChronoUnit.DAYS);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getCursoId() { return cursoId; }
    public UUID getPedidoId() { return pedidoId; }
    public Instant getIniciaEm() { return iniciaEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public Status getStatus() { return status; }
}
