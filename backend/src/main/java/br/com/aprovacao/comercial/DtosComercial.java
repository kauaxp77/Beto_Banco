package br.com.aprovacao.comercial;

import br.com.aprovacao.common.Dinheiro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DtosComercial {

    private DtosComercial() {}

    public record CriarPedidoRequest(
            @NotEmpty(message = "Informe ao menos um curso.") List<UUID> cursoIds,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 120) String nome,
            String whatsapp,
            String cpf,
            String cupom,
            /** Secao 22: aceite de termos registrado com data, hora, IP e versao. */
            @NotBlank String versaoTermos) {}

    /**
     * Resposta do checkout. Devolve o valor tambem formatado para que o frontend
     * nao precise reimplementar a regra de centavos e correr o risco de divergir do
     * que o gateway vai cobrar.
     */
    public record PedidoResponse(
            UUID id,
            String status,
            long valorCentavos,
            long descontoCentavos,
            long valorLiquidoCentavos,
            String valorFormatado,
            String checkoutUrl,
            Instant expiraEm) {

        public static PedidoResponse de(Pedido p) {
            return new PedidoResponse(
                    p.getId(),
                    p.getStatus().name(),
                    p.getValorCentavos(),
                    p.getDescontoCentavos(),
                    p.valorLiquidoCentavos(),
                    Dinheiro.formatar(p.valorLiquidoCentavos()),
                    p.getCheckoutUrl(),
                    p.getExpiraEm());
        }
    }

    /** Secao 19 -- GET /me/matriculas: cursos liberados e validade. */
    public record MatriculaResponse(
            UUID cursoId,
            String cursoTitulo,
            String cursoSlug,
            String status,
            Instant iniciaEm,
            Instant expiraEm,
            long diasRestantes) {}
}
