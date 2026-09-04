package com.betobanco.payments.api;

/**
 * Criacao do link de pagamento e confirmacao do pagamento no provedor.
 * Documento Mestre Premium V3.0, secao 8.
 *
 * <p>Separado de {@link PaymentGateway} porque sao direcoes opostas: aquele
 * recebe e traduz o que o provedor manda; este chama o provedor. Um pode
 * existir sem o outro — em homologacao o webhook e falso e o link e real, e
 * misturar os dois numa interface so tornaria os dois obrigatorios juntos.
 */
public interface CheckoutGateway {

    /** Cria o link de pagamento e devolve para onde mandar o comprador. */
    LinkDeCheckout criarLink(PedidoDeCheckout pedido);

    /**
     * Confirma no provedor que o pagamento existe e esta pago.
     *
     * <p>E o que autentica o webhook. O Checkout Integrado nao documenta
     * assinatura no corpo que envia, entao acreditar nele sem conferir
     * significaria liberar acesso para qualquer um que descubra a URL e mande
     * um JSON com um order_nsu valido.
     */
    Confirmacao confirmar(String orderNsu, String transactionNsu, String invoiceSlug);

    /**
     * O que a InfinitePay precisa saber para montar a fatura.
     *
     * <p>{@code orderNsu} e o id do nosso pedido. Ele volta no webhook e e o
     * unico elo entre o pagamento e quem comprou o que — o webhook nao traz
     * e-mail do comprador nem referencia de produto.
     */
    record PedidoDeCheckout(String orderNsu, String descricao, long valorCentavos,
                            String nome, String email, String telefone) {
    }

    /** {@code slug} vem nulo: o Checkout Integrado so o revela no webhook. */
    record LinkDeCheckout(String url, String slug) {
    }

    record Confirmacao(boolean pago, long valorCentavos, long valorPagoCentavos,
                       int parcelas, String meioDePagamento) {
    }
}
