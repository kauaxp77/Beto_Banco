package br.com.aprovacao.comercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "pedido_item")
public class PedidoItem {

    @Id
    private UUID id;

    /** Preenchido pela associacao em Pedido; aqui e somente leitura. */
    @Column(name = "pedido_id", insertable = false, updatable = false)
    private UUID pedidoId;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    /**
     * Preco no momento da compra. Copiado do curso de proposito: alterar a tabela
     * de precos nao pode reescrever o valor de um pedido ja emitido.
     */
    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    protected PedidoItem() {}

    public PedidoItem(UUID cursoId, long valorCentavos) {
        this.id = UUID.randomUUID();
        this.cursoId = cursoId;
        this.valorCentavos = valorCentavos;
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public UUID getCursoId() { return cursoId; }
    public long getValorCentavos() { return valorCentavos; }
}
