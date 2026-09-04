package com.betobanco.payments.controller;

import com.betobanco.payments.entity.CheckoutOrder;
import com.betobanco.payments.service.CheckoutService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Inicio da compra. Documento Mestre Premium V3.0, secao 8.
 *
 * <p>Publico: quem esta comprando ainda nao tem conta — ela nasce quando o
 * pagamento e aprovado (decisao D4, o aluno recebe link para definir a senha).
 * Exigir login aqui inverteria o funil e mataria a venda.
 *
 * <p>A rota so ABRE o pedido e devolve a URL de pagamento. Nada de acesso
 * acontece aqui: acesso vem do webhook, depois de o provedor confirmar que o
 * pedido foi pago.
 *
 * <p>Secao 21: dado de cartao nunca passa por nos. O comprador digita o cartao
 * no ambiente da InfinitePay, para onde esta URL o leva.
 */
@RestController
@RequestMapping("/checkout")
@Tag(name = "Checkout")
public class CheckoutController {

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping
    @Operation(summary = "Abre o pedido e devolve o link de pagamento da InfinitePay")
    public ResponseEntity<ApiResponse<CheckoutResponse>> abrir(
            @Valid @RequestBody CheckoutRequest req) {

        CheckoutOrder pedido = checkout.abrir(
                req.productId(), req.email(), req.name(), req.whatsapp());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                new CheckoutResponse(pedido.getId(), pedido.getCheckoutUrl(),
                        pedido.getAmountCents())));
    }

    // ------------------------------------------------------------------

    /**
     * Sem preco no corpo, de proposito. O valor sai do catalogo: aceita-lo do
     * cliente deixaria qualquer pessoa comprar a mentoria por um real.
     */
    public record CheckoutRequest(
            @NotNull(message = "Informe o produto")
            UUID productId,

            @NotBlank(message = "Informe seu nome")
            @Size(max = 200)
            String name,

            @NotBlank(message = "Informe seu e-mail")
            @Email(message = "E-mail inválido")
            String email,

            @Size(max = 30)
            String whatsapp) {
    }

    public record CheckoutResponse(UUID orderId, String checkoutUrl, long amountCents) {
    }
}
