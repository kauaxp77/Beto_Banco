package br.com.aprovacao.comercial;

import br.com.aprovacao.comercial.DtosComercial.CriarPedidoRequest;
import br.com.aprovacao.comercial.DtosComercial.PedidoResponse;
import br.com.aprovacao.config.FiltroTenant;
import br.com.aprovacao.config.PropriedadesPlataforma;
import br.com.aprovacao.lgpd.ServicoConsentimento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Secao 19 -- POST /pedidos: cria pedido e devolve link de checkout. */
@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos", description = "Checkout (secoes 12 e 19)")
public class ControladorPedido {

    private final ServicoPedido servico;
    private final ServicoConsentimento consentimento;
    private final PropriedadesPlataforma props;

    public ControladorPedido(ServicoPedido servico, ServicoConsentimento consentimento,
                             PropriedadesPlataforma props) {
        this.servico = servico;
        this.consentimento = consentimento;
        this.props = props;
    }

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Cria pedido e devolve link de checkout da InfinityPay")
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody CriarPedidoRequest req,
            @Parameter(description = "Obrigatorio. Repetir a mesma chave devolve o pedido ja criado.")
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest http) {

        var tenantId = FiltroTenant.atual(props.tenantPadrao());
        Pedido pedido = servico.criar(req, tenantId, idempotencyKey);

        // Secao 22 -- o aceite dos termos e registrado com data, hora, IP e versao.
        // Fica depois da criacao para que o aceite aponte para um pedido que existe.
        consentimento.registrarAceiteDeCompra(tenantId, pedido, req.versaoTermos(), ip(http),
                http.getHeader("User-Agent"));

        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
    }

    private String ip(HttpServletRequest req) {
        String encaminhado = req.getHeader("X-Forwarded-For");
        return (encaminhado != null && !encaminhado.isBlank())
                ? encaminhado.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}
