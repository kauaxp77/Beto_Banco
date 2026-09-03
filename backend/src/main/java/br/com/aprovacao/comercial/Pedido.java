package br.com.aprovacao.comercial;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Secao 18 -- tabela pedido. Registro financeiro: exclusao sempre logica. */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private String email;

    private String nome;
    private String whatsapp;
    private String cpf;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    @Column(name = "desconto_centavos", nullable = false)
    private long descontoCentavos;

    @Column(name = "cupom_id")
    private UUID cupomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status = StatusPedido.PENDENTE;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    // Unidirecional: quem escreve pedido_item.pedido_id e a propria associacao.
    // O campo homonimo em PedidoItem existe so para leitura (insertable/updatable = false),
    // senao Hibernate reclamaria da coluna mapeada duas vezes.
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "pedido_id", nullable = false)
    private List<PedidoItem> itens = new ArrayList<>();

    protected Pedido() {}

    public Pedido(UUID tenantId, String email, String nome, long valorCentavos,
                  String idempotencyKey, int expiraHoras) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.email = email.toLowerCase();
        this.nome = nome;
        this.valorCentavos = valorCentavos;
        this.idempotencyKey = idempotencyKey;
        this.expiraEm = Instant.now().plusSeconds(expiraHoras * 3600L);
    }

    /**
     * Aplica o novo status e diz se ele mudou algo. Devolver false permite ao
     * processador de webhook registrar "evento ja refletido" em vez de reprocessar.
     */
    public boolean mudarStatus(StatusPedido novo) {
        if (this.status == novo) {
            return false;
        }
        this.status = novo;
        this.atualizadoEm = Instant.now();
        return true;
    }

    public boolean expirou() {
        return status == StatusPedido.PENDENTE && expiraEm.isBefore(Instant.now());
    }

    public long valorLiquidoCentavos() {
        return valorCentavos - descontoCentavos;
    }

    public void adicionarItem(UUID cursoId, long valorCentavos) {
        itens.add(new PedidoItem(cursoId, valorCentavos));
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public String getWhatsapp() { return whatsapp; }
    public String getCpf() { return cpf; }
    public long getValorCentavos() { return valorCentavos; }
    public long getDescontoCentavos() { return descontoCentavos; }
    public UUID getCupomId() { return cupomId; }
    public StatusPedido getStatus() { return status; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public List<PedidoItem> getItens() { return itens; }

    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public void setCheckoutUrl(String url) { this.checkoutUrl = url; }
    public void aplicarDesconto(UUID cupomId, long descontoCentavos) {
        this.cupomId = cupomId;
        this.descontoCentavos = descontoCentavos;
    }
}
