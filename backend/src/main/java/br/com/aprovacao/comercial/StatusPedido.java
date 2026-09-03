package br.com.aprovacao.comercial;

/**
 * Secao 12 -- os sete estados do pagamento. A V3.0 tinha quatro; os tres que
 * faltavam (ESTORNADO, CHARGEBACK, EXPIRADO) sao justamente os que decidem quando
 * o acesso e revogado.
 */
public enum StatusPedido {

    /** Aguarda webhook; expira em 72h. Nenhum acesso. */
    PENDENTE(false, false),

    /** Cria usuario e matricula. Libera por 12 meses. */
    APROVADO(true, false),

    /** Cria lead de recuperacao. Nenhum acesso. */
    RECUSADO(false, false),

    /** Encerra o pedido. Revoga. */
    CANCELADO(false, true),

    /** Registra motivo e devolve valor. Revoga imediatamente. */
    ESTORNADO(false, true),

    /** Bloqueia a conta e notifica o financeiro. Revoga e bloqueia recompra. */
    CHARGEBACK(false, true),

    /** Fim dos 12 meses. Revoga com oferta de renovacao. */
    EXPIRADO(false, true);

    private final boolean liberaAcesso;
    private final boolean revogaAcesso;

    StatusPedido(boolean liberaAcesso, boolean revogaAcesso) {
        this.liberaAcesso = liberaAcesso;
        this.revogaAcesso = revogaAcesso;
    }

    public boolean liberaAcesso() {
        return liberaAcesso;
    }

    public boolean revogaAcesso() {
        return revogaAcesso;
    }

    /** CHARGEBACK bloqueia recompra; os demais estados negativos, nao. */
    public boolean bloqueiaRecompra() {
        return this == CHARGEBACK;
    }

    /**
     * Estado final nao volta atras. Usado pelo processador de webhook para
     * descartar um evento antigo que chegou depois de um mais novo (secao 12,
     * "eventos podem chegar fora de ordem").
     */
    public boolean ehFinal() {
        return this == ESTORNADO || this == CHARGEBACK || this == CANCELADO || this == EXPIRADO;
    }
}
