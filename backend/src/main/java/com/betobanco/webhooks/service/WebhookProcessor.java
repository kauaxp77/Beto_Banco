package com.betobanco.webhooks.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.email.api.EmailService;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.payments.api.PaymentGateway;
import com.betobanco.payments.api.PaymentLedger;
import com.betobanco.payments.api.PaymentNotification;
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
    private final PaymentLedger ledger;
    private final Map<String, PaymentGateway> gateways;
    private final ProductCatalog catalogo;
    private final UserDirectory usuarios;
    private final EntitlementService entitlements;
    private final EmailService emails;
    private final AuditLogger auditoria;
    private final boolean habilitado;

    public WebhookProcessor(WebhookEventRepository eventos, WebhookQueue fila,
                            PaymentLedger ledger, List<PaymentGateway> gateways,
                            ProductCatalog catalogo, UserDirectory usuarios,
                            EntitlementService entitlements, EmailService emails,
                            AuditLogger auditoria,
                            @Value("${betobanco.webhooks.processor-enabled:true}")
                            boolean habilitado) {
        this.eventos = eventos;
        this.fila = fila;
        this.ledger = ledger;
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

        PaymentLedger.Registro registro = ledger.registrar(n, evento.getProvider());

        switch (n.tipo()) {
            // Pagamento pendente NAO libera nada. Registrar e tudo.
            case PENDENTE -> ledger.marcarPendente(registro.paymentId());
            case APROVADO -> liberarAcesso(n, registro);
            case CANCELADO -> {
                // Cancelamento ocorre antes de o acesso existir: nada a desfazer.
                ledger.marcarCancelado(registro.paymentId());
                auditoria.registrar("PAYMENT_CANCELLED", "Payment",
                        registro.paymentId().toString(),
                        Map.of("transactionId", n.transactionId()));
            }
            case REEMBOLSADO, CHARGEBACK -> revogarAcesso(n, registro);
            default -> throw new IllegalStateException("tipo nao tratado: " + n.tipo());
        }

        evento.marcarProcessado();
    }

    private void liberarAcesso(PaymentNotification n, PaymentLedger.Registro registro) {
        // SKU desconhecido nao e adivinhado: chutar qual produto liberar e
        // pior do que nao liberar. O evento vai para a fila do administrador.
        var produto = catalogo.buscarPorSku(n.sku()).orElseThrow(
                () -> new IllegalStateException("SKU desconhecido: " + n.sku()));

        boolean contaNova = usuarios.buscarPorEmail(n.buyerEmail()).isEmpty();
        UserAccount aluno = usuarios.buscarPorEmail(n.buyerEmail())
                .orElseGet(() -> usuarios.criarSemSenha(n.buyerEmail(),
                        n.buyerName() == null ? n.buyerEmail() : n.buyerName()));

        ledger.marcarAprovado(registro.paymentId(), aluno.id(), produto.id());

        var concessao = entitlements.conceder(aluno.id(), produto.id(),
                "PAYMENT", registro.paymentId().toString());

        auditoria.registrar("PAYMENT_APPROVED", "Payment", registro.paymentId().toString(),
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

    private void revogarAcesso(PaymentNotification n, PaymentLedger.Registro registro) {
        ledger.marcarEstornado(registro.paymentId(),
                n.tipo() == PaymentNotification.Tipo.CHARGEBACK);

        int revogados = entitlements.revogarPorOrigem(registro.paymentId().toString());

        auditoria.registrar("PAYMENT_REFUNDED", "Payment", registro.paymentId().toString(),
                Map.of("transactionId", n.transactionId(), "entitlementsRevogados", revogados));

        if (revogados > 0) {
            auditoria.registrar("ACCESS_REVOKED", "Payment", registro.paymentId().toString(),
                    Map.of("quantidade", revogados));

            if (registro.userId() != null) {
                usuarios.buscarAtivoPorId(registro.userId()).ifPresent(aluno ->
                        emails.enfileirar(aluno.email(),
                                EmailService.Templates.ACESSO_REVOGADO,
                                Map.of("nome", aluno.fullName()),
                                "acesso-revogado:" + registro.paymentId()));
            }
        }
    }
}
