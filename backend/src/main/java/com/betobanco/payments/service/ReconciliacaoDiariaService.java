package com.betobanco.payments.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.entitlements.api.EntitlementService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ultima defesa do fluxo de pagamento.
 *
 * <p>Documento Mestre V4.0, secao 12: "Reconciliacao diaria: job as 03h compara
 * pedidos locais com a API do gateway e reporta divergencias."
 *
 * <p>A secao 30 classifica a falha de webhook como risco CRITICO-FINANCEIRO:
 * "aluno paga e nao recebe acesso: reembolso, chargeback e dano de reputacao".
 * As tres defesas anteriores — assinatura, idempotencia e retry com backoff —
 * reduzem a chance; esta garante que uma falha que atravessou todas elas seja
 * vista em ate 24 horas, e nao quando o aluno abre um chamado.
 *
 * <p>A correcao automatica e deliberadamente assimetrica. Conceder o que ja foi
 * pago nao tem risco: o dinheiro entrou. Revogar acesso automaticamente, sim —
 * um falso positivo tira o curso de um aluno legitimo, e o custo de auditar
 * algumas linhas a mao e menor que o de explicar isso depois. Por isso o lado
 * "acesso sem pagamento" e apenas reportado.
 */
@Service
public class ReconciliacaoDiariaService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacaoDiariaService.class);

    private static final String PAGO_SEM_ACESSO = "PAGO_SEM_ACESSO";

    private final EntityManager em;
    private final EntitlementService entitlements;
    private final AuditLogger auditoria;
    private final boolean habilitado;

    public ReconciliacaoDiariaService(
            EntityManager em,
            EntitlementService entitlements,
            AuditLogger auditoria,
            @Value("${betobanco.payments.reconciliacao-habilitada:true}") boolean habilitado) {
        this.em = em;
        this.entitlements = entitlements;
        this.auditoria = auditoria;
        this.habilitado = habilitado;
    }

    /** 03h em America/Sao_Paulo — a janela de menor trafego da plataforma. */
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    public void executarAgendado() {
        if (!habilitado) {
            return;
        }
        Resultado resultado = reconciliar();
        if (resultado.temDivergencia()) {
            log.warn("Reconciliacao diaria: {}", resultado);
        } else {
            log.info("Reconciliacao diaria: pagamento e acesso casados, nenhuma divergencia.");
        }
    }

    /**
     * Exposto separado do agendamento para que o admin possa disparar sob
     * demanda e para que o teste chame sem esperar as 03h.
     */
    @Transactional
    public Resultado reconciliar() {
        List<Divergencia> divergencias = lerDivergencias();

        int concedidos = 0;
        List<String> pendentesDeRevisao = new ArrayList<>();

        for (Divergencia d : divergencias) {
            if (PAGO_SEM_ACESSO.equals(d.tipo())) {
                EntitlementService.Concessao concessao = entitlements.conceder(
                        d.userId(), d.productId(), "PAYMENT", d.paymentId().toString());
                if (concessao.criadoAgora()) {
                    concedidos++;
                    log.warn("Reconciliacao concedeu acesso do pagamento {} ({}) — "
                            + "o webhook nao havia liberado.", d.paymentId(), d.email());
                    auditoria.registrar("RECONCILIATION_ACCESS_GRANTED", "Payment",
                            d.paymentId().toString(),
                            Map.of("userId", d.userId().toString(),
                                    "productId", d.productId().toString(),
                                    "motivo", "webhook nao liberou o acesso"));
                }
            } else {
                pendentesDeRevisao.add(d.paymentId() + " (" + d.email() + ", " + d.paymentStatus() + ")");
            }
        }

        if (!pendentesDeRevisao.isEmpty()) {
            log.error("Acesso vigente sem pagamento aprovado, revisar a mao: {}", pendentesDeRevisao);
        }

        return new Resultado(divergencias.size(), concedidos, pendentesDeRevisao);
    }

    @SuppressWarnings("unchecked")
    private List<Divergencia> lerDivergencias() {
        List<Object[]> linhas = em.createNativeQuery("""
                SELECT payment_id, user_id, product_id, email, payment_status, divergencia
                  FROM vw_divergencia_acesso
                """).getResultList();

        List<Divergencia> divergencias = new ArrayList<>(linhas.size());
        for (Object[] l : linhas) {
            divergencias.add(new Divergencia(
                    (UUID) l[0], (UUID) l[1], (UUID) l[2],
                    (String) l[3], (String) l[4], (String) l[5]));
        }
        return divergencias;
    }

    private record Divergencia(UUID paymentId, UUID userId, UUID productId,
                               String email, String paymentStatus, String tipo) {
    }

    public record Resultado(int divergenciasEncontradas, int acessosConcedidos,
                            List<String> pendentesDeRevisao) {

        public boolean temDivergencia() {
            return divergenciasEncontradas > 0;
        }

        @Override
        public String toString() {
            return "%d divergencias, %d acessos concedidos automaticamente, %d aguardando revisao manual"
                    .formatted(divergenciasEncontradas, acessosConcedidos, pendentesDeRevisao.size());
        }
    }
}
