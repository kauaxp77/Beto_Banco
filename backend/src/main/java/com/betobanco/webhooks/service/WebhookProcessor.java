package com.betobanco.webhooks.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.email.api.EmailService;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.payments.api.PaymentGateway;
import com.betobanco.payments.api.PaymentNotification;
import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.entity.PaymentSplit;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.payments.repository.PaymentSplitRepository;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Estagio 2 do fluxo de pagamento: processamento.
 *
 * <p>Roda em background porque o estagio 1 precisa responder ao gateway em
 * milissegundos. Tudo o que acontece aqui — criar aluno, conceder acesso,
 * gravar auditoria — cabe numa unica transacao, <b>sem nenhuma chamada
 * externa dentro dela</b>. O e-mail vai para a outbox, nao para o servidor
 * SMTP: mensagem enviada nao tem rollback.
 */
@Service
public class WebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessor.class);
    private static final int LOTE = 10;

    private final WebhookEventRepository eventos;
    private final WebhookQueue fila;
    private final PaymentRepository pagamentos;
    private final PaymentSplitRepository splits;
    private final Map<String, PaymentGateway> gateways;
    private final ProductCatalog catalogo;
    private final UserDirectory usuarios;
    private final EntitlementService entitlements;
    private final EmailService emails;
    private final AuditLogger auditoria;
    private final boolean habilitado;

    public WebhookProcessor(WebhookEventRepository eventos, WebhookQueue fila,
                            PaymentRepository pagamentos,
                            PaymentSplitRepository splits, List<PaymentGateway> gateways,
                            ProductCatalog catalogo, UserDirectory usuarios,
                            EntitlementService entitlements, EmailService emails,
                            AuditLogger auditoria,
                            @Value("${betobanco.webhooks.processor-enabled:true}")
                            boolean habilitado) {
        this.eventos = eventos;
        this.fila = fila;
        this.pagamentos = pagamentos;
        this.splits = splits;
        this.gateways = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::provider, Function.identity()));
        this.catalogo = catalogo;
        this.usuarios = usuarios;
        this.entitlements = entitlements;
        this.emails = emails;
        this.auditoria = auditoria;
        this.habilitado = habilitado;
    }

    @Scheduled(fixedDelayString = "${betobanco.webhooks.process-interval-ms:10000}")
    public void processarAgendado() {
        if (!habilitado) {
            return;
        }
        try {
            processarLote();
        } catch (Exception e) {
            log.error("Falha inesperada no processamento de webhooks", e);
        }
    }

    /** Processa um lote de eventos pendentes. Devolve quantos foram tratados. */
    public int processarLote() {
        List<UUID> ids = fila.proximosIds(LOTE);
        int tratados = 0;
        for (UUID id : ids) {
            if (processarUm(id)) {
                tratados++;
            }
        }
        return tratados;
    }

    /**
     * Uma transacao por evento: uma falha isolada nao derruba o lote inteiro,
     * e o retry recomeça do zero para aquele evento apenas.
     */
    @Transactional
    public boolean processarUm(UUID eventoId) {
        Optional<WebhookEvent> encontrado = eventos.findById(eventoId);
        if (encontrado.isEmpty()) {
            return false;
        }
        WebhookEvent evento = encontrado.get();

        try {
            PaymentGateway gateway = gateways.get(evento.getProvider());
            if (gateway == null) {
                evento.registrarFalha("provedor desconhecido: " + evento.getProvider());
                eventos.saveAndFlush(evento);
                return false;
            }

            Optional<PaymentNotification> interpretado =
                    gateway.interpretar(evento.getPayload().getBytes(StandardCharsets.UTF_8));
            if (interpretado.isEmpty()) {
                evento.registrarFalha("payload ilegivel");
                eventos.saveAndFlush(evento);
                return false;
            }

            aplicar(interpretado.get(), evento);
            eventos.saveAndFlush(evento);
            return true;

        } catch (Exception e) {
            log.warn("Falha ao processar evento {}: {}", evento.getEventId(), e.getMessage());
            evento.registrarFalha(e.getMessage());
            eventos.saveAndFlush(evento);
            return false;
        }
    }

    private void aplicar(PaymentNotification n, WebhookEvent evento) {
        if (n.tipo() == PaymentNotification.Tipo.IGNORADO) {
            evento.marcarIgnorado("evento sem efeito no dominio: " + n.eventType());
            return;
        }

        Payment pagamento = registrarPagamento(n, evento.getProvider());

        switch (n.tipo()) {
            case APROVADO -> liberarAcesso(n, pagamento);
            case PENDENTE -> {
                // Pagamento pendente NAO libera nada. Registrar e tudo.
                pagamento.setStatus(Payment.PENDING);
                pagamentos.saveAndFlush(pagamento);
            }
            case CANCELADO -> {
                // Cancelamento ocorre antes de o acesso existir: nada a desfazer.
                pagamento.setStatus(Payment.CANCELLED);
                pagamentos.saveAndFlush(pagamento);
                auditoria.registrar("PAYMENT_CANCELLED", "Payment",
                        pagamento.getId().toString(), Map.of("transactionId", n.transactionId()));
            }
            case REEMBOLSADO, CHARGEBACK -> revogarAcesso(n, pagamento);
            default -> throw new IllegalStateException("tipo nao tratado: " + n.tipo());
        }

        evento.marcarProcessado();
    }

    private Payment registrarPagamento(PaymentNotification n, String provider) {
        Payment pagamento = pagamentos
                .findByProviderAndProviderTransactionId(provider, n.transactionId())
                .orElseGet(() -> new Payment(provider, n.transactionId(), n.buyerEmail(),
                        n.amountCents(), Payment.PENDING));

        pagamento.setBuyerName(n.buyerName());
        pagamento.setCurrency(n.currency());
        pagamentos.saveAndFlush(pagamento);

        if (!n.splits().isEmpty() && splits.countByPaymentId(pagamento.getId()) == 0) {
            for (PaymentNotification.Split s : n.splits()) {
                splits.save(new PaymentSplit(pagamento.getId(), s.recipient(),
                        s.amountCents(), s.percentage()));
            }
            splits.flush();
        }

        return pagamento;
    }

    private void liberarAcesso(PaymentNotification n, Payment pagamento) {
        // SKU desconhecido nao e adivinhado: chutar qual produto liberar e
        // pior do que nao liberar. O evento vai para a fila do administrador.
        var produto = catalogo.buscarPorSku(n.sku()).orElseThrow(
                () -> new IllegalStateException("SKU desconhecido: " + n.sku()));

        boolean contaNova = usuarios.buscarPorEmail(n.buyerEmail()).isEmpty();
        UserAccount aluno = usuarios.buscarPorEmail(n.buyerEmail())
                .orElseGet(() -> usuarios.criarSemSenha(n.buyerEmail(),
                        n.buyerName() == null ? n.buyerEmail() : n.buyerName()));

        pagamento.setUserId(aluno.id());
        pagamento.setProductId(produto.id());
        pagamento.setStatus(Payment.APPROVED);
        pagamento.setApprovedAt(Instant.now());
        pagamentos.saveAndFlush(pagamento);

        var concessao = entitlements.conceder(aluno.id(), produto.id(),
                "PAYMENT", pagamento.getId().toString());

        auditoria.registrar("PAYMENT_APPROVED", "Payment", pagamento.getId().toString(),
                Map.of("transactionId", n.transactionId(), "sku", n.sku()));

        if (concessao.criadoAgora()) {
            auditoria.registrar("ACCESS_GRANTED", "Entitlement",
                    concessao.entitlementId().toString(),
                    Map.of("userId", aluno.id().toString(), "productId", produto.id().toString()));
        }

        // O e-mail vai para a outbox, nunca enviado aqui dentro.
        if (contaNova) {
            emails.enfileirar(aluno.email(), EmailService.Templates.PRIMEIRO_ACESSO,
                    Map.of("nome", aluno.fullName(), "userId", aluno.id().toString()),
                    "primeiro-acesso:" + aluno.id());
        } else {
            emails.enfileirar(aluno.email(), EmailService.Templates.ACESSO_LIBERADO,
                    Map.of("nome", aluno.fullName(), "produto", produto.name()),
                    "acesso-liberado:" + concessao.entitlementId());
        }
    }

    private void revogarAcesso(PaymentNotification n, Payment pagamento) {
        pagamento.setStatus(n.tipo() == PaymentNotification.Tipo.CHARGEBACK
                ? Payment.CHARGEBACK : Payment.REFUNDED);
        pagamentos.saveAndFlush(pagamento);

        int revogados = entitlements.revogarPorOrigem(pagamento.getId().toString());

        auditoria.registrar("PAYMENT_REFUNDED", "Payment", pagamento.getId().toString(),
                Map.of("transactionId", n.transactionId(), "entitlementsRevogados", revogados));

        if (revogados > 0) {
            auditoria.registrar("ACCESS_REVOKED", "Payment", pagamento.getId().toString(),
                    Map.of("quantidade", revogados));

            if (pagamento.getUserId() != null) {
                usuarios.buscarAtivoPorId(pagamento.getUserId()).ifPresent(aluno ->
                        emails.enfileirar(aluno.email(),
                                EmailService.Templates.ACESSO_REVOGADO,
                                Map.of("nome", aluno.fullName()),
                                "acesso-revogado:" + pagamento.getId()));
            }
        }
    }
}
