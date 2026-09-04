package com.betobanco.payments.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato que o modulo {@code payments} publica sobre o pedido de checkout.
 * Documento Mestre Premium V3.0, secao 8.
 *
 * <p>Existe porque o webhook do Checkout Integrado da InfinitePay nao diz quem
 * comprou nem o que: ele traz {@code order_nsu}. Quem processa o webhook precisa
 * transformar isso em "produto X para o comprador Y", e essa traducao envolve
 * dinheiro — entao mora aqui, e nao no modulo de webhooks.
 */
public interface CheckoutOrders {

    /**
     * Reencontra o pedido, confirma no provedor que ele foi pago e o marca como
     * pago.
     *
     * <p>Devolve vazio quando o pedido nao existe <b>ou</b> quando o provedor
     * nao confirma o pagamento. Vazio significa <b>nao liberar acesso</b>: e a
     * regra de que so recebe o curso quem pagou por ele, e ela e verificada
     * contra o provedor, nao contra o que o webhook afirma.
     */
    Optional<CompraConfirmada> confirmarPagamento(String orderNsu, String invoiceSlug,
                                                  String transactionNsu);

    record CompraConfirmada(UUID orderId, UUID productId, String buyerEmail,
                            String buyerName, long amountCents) {
    }
}
