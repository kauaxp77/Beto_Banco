package br.com.aprovacao.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Secao 18 -- tabela curso. */
@Entity
@Table(name = "curso")
public class Curso {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "carreira_id")
    private UUID carreiraId;

    @Column(name = "cargo_id")
    private UUID cargoId;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String slug;

    private String subtitulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "capa_storage_key")
    private String capaStorageKey;

    @Column(name = "preco_centavos", nullable = false)
    private long precoCentavos;

    /** Secao 03 -- 12 meses. Fica no curso para que um combo possa ter prazo proprio. */
    @Column(name = "dias_acesso", nullable = false)
    private int diasAcesso = 365;

    /** Secao 14 -- "Curso avulso: 1 [correcao] na compra." */
    @Column(name = "cota_redacao_compra", nullable = false)
    private short cotaRedacaoCompra = 1;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Curso() {}

    public boolean estaPublicado() {
        return publicadoEm != null && publicadoEm.isBefore(Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCarreiraId() { return carreiraId; }
    public UUID getCargoId() { return cargoId; }
    public String getTitulo() { return titulo; }
    public String getSlug() { return slug; }
    public String getSubtitulo() { return subtitulo; }
    public String getDescricao() { return descricao; }
    public String getCapaStorageKey() { return capaStorageKey; }
    public long getPrecoCentavos() { return precoCentavos; }
    public int getDiasAcesso() { return diasAcesso; }
    public short getCotaRedacaoCompra() { return cotaRedacaoCompra; }
    public Instant getPublicadoEm() { return publicadoEm; }
}
