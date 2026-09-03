package br.com.aprovacao.comercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Secao 03: "Cupons tem teto de 30% e validade obrigatoria -- sem cupom eterno."
 */
@Entity
@Table(name = "cupom")
public class Cupom {

    public static final int PERCENTUAL_MAXIMO = 30;

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private short percentual;

    @Column(name = "usos_max", nullable = false)
    private int usosMax;

    @Column(nullable = false)
    private int usos;

    @Column(name = "valido_ate", nullable = false)
    private Instant validoAte;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    /**
     * Trava otimista no consumo. Sem ela, dois checkouts simultaneos leem usos = N,
     * gravam N + 1 os dois, e um cupom de uso unico e aplicado duas vezes.
     */
    @Version
    @Column(name = "versao")
    private long versao;

    protected Cupom() {}

    public Cupom(UUID tenantId, String codigo, short percentual, int usosMax, Instant validoAte) {
        if (percentual <= 0 || percentual > PERCENTUAL_MAXIMO) {
            throw new IllegalArgumentException(
                    "Secao 03: cupom tem teto de " + PERCENTUAL_MAXIMO + "%. Recebido: " + percentual);
        }
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.codigo = codigo.toUpperCase();
        this.percentual = percentual;
        this.usosMax = usosMax;
        this.validoAte = validoAte;
    }

    public boolean utilizavel() {
        return usos < usosMax && validoAte.isAfter(Instant.now());
    }

    public void consumir() {
        if (!utilizavel()) {
            throw new IllegalStateException("Cupom " + codigo + " esgotado ou vencido.");
        }
        usos++;
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public short getPercentual() { return percentual; }
    public Instant getValidoAte() { return validoAte; }
    public int getUsos() { return usos; }
    public int getUsosMax() { return usosMax; }
}
