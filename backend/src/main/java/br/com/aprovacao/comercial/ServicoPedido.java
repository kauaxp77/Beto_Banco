package br.com.aprovacao.comercial;

import br.com.aprovacao.catalogo.Curso;
import br.com.aprovacao.catalogo.CursoRepository;
import br.com.aprovacao.common.Dinheiro;
import br.com.aprovacao.common.ProblemaNegocio;
import br.com.aprovacao.config.PropriedadesPlataforma;
import br.com.aprovacao.pagamento.ClienteInfinityPay;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 12 -- passos 1 a 3 do fluxo de compra: escolha do curso, checkout e
 * redirecionamento para a InfinityPay. Os passos 4 a 6 (webhook, conta criada,
 * acesso liberado) sao do {@code ProcessadorWebhook}, de proposito: nada aqui
 * libera acesso.
 */
@Service
public class ServicoPedido {

    private static final Logger log = LoggerFactory.getLogger(ServicoPedido.class);

    private final PedidoRepository pedidos;
    private final CursoRepository cursos;
    private final CupomRepository cupons;
    private final ClienteInfinityPay gateway;
    private final PropriedadesPlataforma props;

    public ServicoPedido(PedidoRepository pedidos,
                         CursoRepository cursos,
                         CupomRepository cupons,
                         ClienteInfinityPay gateway,
                         PropriedadesPlataforma props) {
        this.pedidos = pedidos;
        this.cursos = cursos;
        this.cupons = cupons;
        this.gateway = gateway;
        this.props = props;
    }

    /**
     * Secao 19: "Idempotency-Key obrigatorio em todo POST que cria pedido ou
     * pagamento."
     *
     * <p>Sem ela, um duplo clique no botao de comprar gera dois pedidos e dois
     * links de checkout, e o aluno pode pagar os dois.
     */
    @Transactional
    public Pedido criar(DtosComercial.CriarPedidoRequest req, UUID tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ProblemaNegocio.invalido("idempotency-key-ausente",
                    "O cabecalho Idempotency-Key e obrigatorio nesta rota.");
        }

        Optional<Pedido> jaExiste = pedidos.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
        if (jaExiste.isPresent()) {
            return jaExiste.get();
        }

        List<Curso> comprados = new ArrayList<>();
        long bruto = 0;
        for (UUID cursoId : req.cursoIds()) {
            Curso curso = cursos.findById(cursoId)
                    .filter(c -> c.getTenantId().equals(tenantId))
                    .filter(Curso::estaPublicado)
                    .orElseThrow(() -> ProblemaNegocio.naoEncontrado("Curso " + cursoId));
            comprados.add(curso);
            bruto += curso.getPrecoCentavos();
        }
        if (comprados.isEmpty()) {
            throw ProblemaNegocio.invalido("pedido-vazio", "Informe ao menos um curso.");
        }

        Pedido pedido = new Pedido(tenantId, req.email(), req.nome(), bruto,
                idempotencyKey, props.pagamento().pedidoExpiraHoras());
        pedido.setWhatsapp(req.whatsapp());
        pedido.setCpf(req.cpf());
        comprados.forEach(c -> pedido.adicionarItem(c.getId(), c.getPrecoCentavos()));

        aplicarCupom(pedido, req.cupom(), tenantId, bruto);

        pedidos.saveAndFlush(pedido);

        // O link de checkout so e pedido depois de o pedido existir no banco: se a
        // chamada ao gateway falhar, ainda temos o registro para retentar, em vez de
        // uma cobranca sem pedido do nosso lado.
        String checkout = gateway.criarCheckout(pedido);
        pedido.setCheckoutUrl(checkout);

        log.info("Pedido {} criado: {} ({} itens).", pedido.getId(),
                Dinheiro.formatar(pedido.valorLiquidoCentavos()), pedido.getItens().size());
        return pedido;
    }

    private void aplicarCupom(Pedido pedido, String codigo, UUID tenantId, long bruto) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }
        Cupom cupom = cupons.buscarPorCodigo(tenantId, codigo)
                .orElseThrow(() -> ProblemaNegocio.invalido("cupom-invalido", "Cupom nao encontrado."));
        if (!cupom.utilizavel()) {
            throw ProblemaNegocio.invalido("cupom-expirado", "Este cupom esta esgotado ou fora da validade.");
        }
        cupom.consumir();
        pedido.aplicarDesconto(cupom.getId(), Dinheiro.aplicarPercentual(bruto, cupom.getPercentual()));
    }
}
