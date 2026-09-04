package com.betobanco.payments.service;

import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.payments.api.CheckoutGateway;
import com.betobanco.payments.api.CheckoutOrders;
import com.betobanco.payments.entity.CheckoutOrder;
import com.betobanco.payments.repository.CheckoutOrderRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Criacao do pedido e confirmacao do pagamento. Documento Mestre Premium V3.0,
 * secao 8: "Aluno escolhe curso -> Checkout -> InfinityPay -> Webhook ->
 * Criacao de usuario -> Liberacao automatica".
 */
@Service
public class CheckoutService implements CheckoutOrders {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CheckoutOrderRepository pedidos;
    private final CheckoutGateway gateway;
    private final ProductCatalog catalogo;
    private final boolean confirmarAntesDeLiberar;

    public CheckoutService(CheckoutOrderRepository pedidos, CheckoutGateway gateway,
                           ProductCatalog catalogo,
                           @Value("${betobanco.payments.infinitypay.confirmar-antes-de-liberar:true}")
                           boolean confirmarAntesDeLiberar) {
        this.pedidos = pedidos;
        this.gateway = gateway;
        this.catalogo = catalogo;
        this.confirmarAntesDeLiberar = confirmarAntesDeLiberar;
    }

    /**
     * Abre o pedido e devolve para onde mandar o comprador.
     *
     * <p>O preco vem do catalogo, nunca do cliente. Aceitar valor vindo da tela
     * deixaria qualquer pessoa comprar a mentoria de R$ 3.564 por um real —
     * bastaria trocar um numero antes de enviar o formulario.
     */
    @Transactional
    public CheckoutOrder abrir(UUID productId, String email, String nome, String telefone) {
        ProductCatalog.ProductSummary produto = catalogo.buscarPorId(productId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        if (!produto.active()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Este produto não está à venda no momento.");
        }
        if (produto.priceCents() <= 0) {
            // Produto gratuito nao passa por checkout: liberar acesso de graca
            // e concessao de entitlement, nao venda.
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Produto sem preço definido não pode ser vendido pelo checkout.");
        }

        CheckoutOrder pedido = pedidos.saveAndFlush(new CheckoutOrder(
                produto.id(), email, nome, telefone, produto.priceCents()));

        CheckoutGateway.LinkDeCheckout link = gateway.criarLink(
                new CheckoutGateway.PedidoDeCheckout(
                        pedido.getId().toString(), produto.name(), produto.priceCents(),
                        nome, pedido.getBuyerEmail(), telefone));

        pedido.registrarLink(link.url(), link.slug());
        return pedidos.save(pedido);
    }

    /**
     * {@inheritDoc}
     *
     * <p>DESVIO CONSCIENTE: o {@link com.betobanco.webhooks.service.WebhookProcessor}
     * documenta que nao ha chamada externa dentro da transacao de processamento,
     * e esta confirmacao e uma. Ela fica assim mesmo porque a alternativa e pior:
     * o Checkout Integrado nao documenta assinatura no corpo que envia, entao
     * sem perguntar ao provedor "este pedido foi mesmo pago?" bastaria descobrir
     * a URL do webhook e mandar um JSON com um order_nsu valido para receber um
     * curso de graça. Os tempos limite do cliente HTTP sao curtos (5s e 10s) e a
     * transacao e uma por evento.
     */
    @Override
    @Transactional
    public Optional<CompraConfirmada> confirmarPagamento(String orderNsu, String invoiceSlug,
                                                         String transactionNsu) {
        Optional<CheckoutOrder> encontrado = porReferencia(orderNsu, invoiceSlug);
        if (encontrado.isEmpty()) {
            log.warn("Webhook citou pedido desconhecido (order_nsu={}, slug={}); "
                    + "nenhum acesso liberado.", orderNsu, invoiceSlug);
            return Optional.empty();
        }

        CheckoutOrder pedido = encontrado.get();

        if (confirmarAntesDeLiberar && !confirmado(pedido, transactionNsu, invoiceSlug)) {
            return Optional.empty();
        }

        pedido.marcarPago(transactionNsu, invoiceSlug);
        pedidos.save(pedido);

        return Optional.of(new CompraConfirmada(pedido.getId(), pedido.getProductId(),
                pedido.getBuyerEmail(), pedido.getBuyerName(), pedido.getAmountCents()));
    }

    // ------------------------------------------------------------------

    private boolean confirmado(CheckoutOrder pedido, String transactionNsu, String invoiceSlug) {
        String slug = invoiceSlug != null ? invoiceSlug : pedido.getInvoiceSlug();

        CheckoutGateway.Confirmacao confirmacao = gateway.confirmar(
                pedido.getId().toString(), transactionNsu, slug);

        if (!confirmacao.pago()) {
            log.warn("Pedido {} nao esta pago segundo a InfinitePay; acesso nao liberado.",
                    pedido.getId());
            return false;
        }

        // Pagou menos do que o pedido cobrava: nao libera. O valor conferido e o
        // que o provedor diz ter cobrado, nao o que o webhook afirma.
        if (confirmacao.valorCentavos() < pedido.getAmountCents()) {
            log.error("Pedido {} confirmado por {} centavos, abaixo dos {} cobrados. "
                            + "Acesso NAO liberado; caso para o administrador.",
                    pedido.getId(), confirmacao.valorCentavos(), pedido.getAmountCents());
            return false;
        }
        return true;
    }

    /**
     * O {@code order_nsu} e o id do nosso pedido. Quando ele nao volta — fatura
     * criada fora da plataforma, por exemplo — o slug e o que sobra.
     */
    private Optional<CheckoutOrder> porReferencia(String orderNsu, String invoiceSlug) {
        if (orderNsu != null && !orderNsu.isBlank()) {
            try {
                Optional<CheckoutOrder> porId = pedidos.findById(UUID.fromString(orderNsu.trim()));
                if (porId.isPresent()) {
                    return porId;
                }
            } catch (IllegalArgumentException naoEhUuid) {
                log.warn("order_nsu '{}' nao e um identificador de pedido desta plataforma.",
                        orderNsu);
            }
        }
        if (invoiceSlug != null && !invoiceSlug.isBlank()) {
            return pedidos.findByInvoiceSlug(invoiceSlug);
        }
        return Optional.empty();
    }
}
