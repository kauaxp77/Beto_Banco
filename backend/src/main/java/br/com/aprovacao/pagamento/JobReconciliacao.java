package br.com.aprovacao.pagamento;

import br.com.aprovacao.comercial.Pedido;
import br.com.aprovacao.comercial.PedidoRepository;
import br.com.aprovacao.comercial.StatusPedido;
import br.com.aprovacao.consumo.MatriculaRepository;
import br.com.aprovacao.lgpd.ServicoEmail;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 12 -- "Reconciliacao diaria: job as 03h compara pedidos locais com a API do
 * gateway e reporta divergencias."
 *
 * <p>Secao 30 classifica a falha de webhook como risco CRITICO-FINANCEIRO. As tres
 * defesas anteriores (idempotencia, ordem, backoff) reduzem a chance; esta e a que
 * garante que uma falha que atravessou todas elas seja vista em ate 24h, e nao
 * quando o aluno abre um chamado.
 */
@Component
public class JobReconciliacao {

    private static final Logger log = LoggerFactory.getLogger(JobReconciliacao.class);

    private final PedidoRepository pedidos;
    private final MatriculaRepository matriculas;
    private final ServicoAcesso acesso;
    private final ServicoEmail email;
    private final EntityManager em;

    public JobReconciliacao(PedidoRepository pedidos,
                            MatriculaRepository matriculas,
                            ServicoAcesso acesso,
                            ServicoEmail email,
                            EntityManager em) {
        this.pedidos = pedidos;
        this.matriculas = matriculas;
        this.acesso = acesso;
        this.email = email;
        this.em = em;
    }

    /** 03h em America/Sao_Paulo -- janela de menor trafego. */
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void reconciliar() {
        int pendentesExpirados = expirarPendentes();
        int matriculasExpiradas = matriculas.expirarVencidas(Instant.now());
        List<Object[]> divergencias = lerDivergencias();

        log.info("Reconciliacao diaria: {} pedidos expirados, {} matriculas expiradas, {} divergencias.",
                pendentesExpirados, matriculasExpiradas, divergencias.size());

        // Autocorrecao apenas do lado seguro: pedido APROVADO sem matricula viva
        // recebe o acesso que ja foi pago. O caminho inverso (acesso sem pagamento)
        // nunca e corrigido automaticamente -- revogar por engano custaria mais do
        // que a auditoria manual de um punhado de linhas.
        int corrigidos = 0;
        for (Object[] linha : divergencias) {
            if ("PAGO_SEM_ACESSO".equals(String.valueOf(linha[3]))) {
                Pedido pedido = pedidos.findById((java.util.UUID) linha[0]).orElse(null);
                if (pedido != null && pedido.getStatus() == StatusPedido.APROVADO) {
                    acesso.liberarAcesso(pedido);
                    corrigidos++;
                }
            }
        }

        if (!divergencias.isEmpty()) {
            email.alertarFinanceiro("Reconciliacao diaria com divergencias",
                    "%d divergencias entre pedido e acesso. %d corrigidas automaticamente (pago sem acesso). "
                    .formatted(divergencias.size(), corrigidos)
                    + "As demais sao acesso sem pagamento aprovado e exigem revisao manual em vw_divergencia_acesso.");
        }
    }

    /** Secao 12 -- PENDENTE "aguarda webhook; expira em 72h". */
    private int expirarPendentes() {
        List<Pedido> vencidos = pedidos.pendentesVencidos(Instant.now());
        vencidos.forEach(p -> p.mudarStatus(StatusPedido.EXPIRADO));
        pedidos.saveAll(vencidos);
        return vencidos.size();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> lerDivergencias() {
        return em.createNativeQuery(
                "SELECT pedido_id, email, pedido_status, divergencia FROM vw_divergencia_acesso")
                .getResultList();
    }
}
