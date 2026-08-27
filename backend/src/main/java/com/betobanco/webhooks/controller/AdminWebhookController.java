package com.betobanco.webhooks.controller;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.auth.api.FirstAccessTokens;
import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.email.api.EmailService;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.pagination.PageRequestFactory;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.response.PageResponse;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.webhooks.dto.ResolveManuallyRequest;
import com.betobanco.webhooks.dto.WebhookEventAdminResponse;
import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A fila de atencao do administrador (spec 7.4): eventos que o worker nao
 * conseguiu processar. Reprocessar devolve para a fila; resolver manualmente
 * faz na mao o que o worker faria — e encerra o evento.
 */
@RestController
@RequestMapping("/admin/webhooks")
@Tag(name = "Admin - Webhooks")
public class AdminWebhookController {

    /** So faz sentido intervir no que o worker ja desistiu de resolver. */
    private static final Set<String> INTERVENIVEIS =
            Set.of(WebhookEvent.FAILED, WebhookEvent.MANUAL);

    private final WebhookEventRepository eventos;
    private final ProductCatalog catalogo;
    private final UserDirectory usuarios;
    private final EntitlementService entitlements;
    private final EmailService emails;
    private final FirstAccessTokens primeiroAcesso;
    private final AuditLogger auditoria;

    public AdminWebhookController(WebhookEventRepository eventos, ProductCatalog catalogo,
                                  UserDirectory usuarios, EntitlementService entitlements,
                                  EmailService emails, FirstAccessTokens primeiroAcesso,
                                  AuditLogger auditoria) {
        this.eventos = eventos;
        this.catalogo = catalogo;
        this.usuarios = usuarios;
        this.entitlements = entitlements;
        this.emails = emails;
        this.primeiroAcesso = primeiroAcesso;
        this.auditoria = auditoria;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<WebhookEventAdminResponse>> listar(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        var paginacao = PageRequestFactory.of(page, size, null);
        var pagina = (status == null || status.isBlank())
                ? eventos.findAllByOrderByReceivedAtDesc(paginacao)
                : eventos.findByStatusInOrderByReceivedAtDesc(List.of(status.trim()), paginacao);

        return ResponseEntity.ok(PageResponse.from(pagina.map(WebhookEventAdminResponse::from)));
    }

    @PostMapping("/{id}/reprocess")
    @Transactional
    public ResponseEntity<ApiResponse<WebhookEventAdminResponse>> reprocessar(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable("id") UUID id) {

        // findById inline: a regra ArchUnit nenhumControllerRetornaEntidadeJpa
        // reprova qualquer metodo desta classe, mesmo privado, que devolva a
        // entidade — o helper so pode validar, nunca retornar.
        WebhookEvent evento = eventos.findById(id)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado"));
        validarIntervenivel(evento);
        evento.reenfileirar();
        eventos.saveAndFlush(evento);

        auditoria.registrarComAtor(admin.id(), AuditLogger.Acoes.WEBHOOK_REPROCESSED,
                "WebhookEvent", id.toString(), Map.of("eventId", evento.getEventId()));

        return ResponseEntity.ok(ApiResponse.ok(WebhookEventAdminResponse.from(evento)));
    }

    /**
     * Uma transacao de dominio, como no worker: aluno, concessao, auditoria e
     * outbox juntos — nenhuma chamada externa aqui dentro.
     */
    @PostMapping("/{id}/resolve-manually")
    @Transactional
    public ResponseEntity<ApiResponse<WebhookEventAdminResponse>> resolverManualmente(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ResolveManuallyRequest req) {

        WebhookEvent evento = eventos.findById(id)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado"));
        validarIntervenivel(evento);
        var produto = catalogo.buscarPorId(req.productId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        boolean contaNova = usuarios.buscarPorEmail(req.email()).isEmpty();
        UserAccount aluno = usuarios.buscarPorEmail(req.email())
                .orElseGet(() -> usuarios.criarSemSenha(req.email(), req.email()));

        var concessao = entitlements.conceder(aluno.id(), produto.id(),
                "MANUAL", "webhook:" + id);

        if (contaNova) {
            String token = primeiroAcesso.criarPara(aluno);
            emails.enfileirar(aluno.email(), EmailService.Templates.PRIMEIRO_ACESSO,
                    Map.of("nome", aluno.fullName(), "userId", aluno.id().toString(),
                            "token", token),
                    "primeiro-acesso:" + aluno.id());
        } else {
            emails.enfileirar(aluno.email(), EmailService.Templates.ACESSO_LIBERADO,
                    Map.of("nome", aluno.fullName(), "produto", produto.name()),
                    "acesso-liberado:" + concessao.entitlementId());
        }

        evento.marcarProcessado();
        eventos.saveAndFlush(evento);

        auditoria.registrarComAtor(admin.id(), AuditLogger.Acoes.WEBHOOK_RESOLVED_MANUALLY,
                "WebhookEvent", id.toString(),
                Map.of("email", aluno.email(), "productId", produto.id().toString(),
                        "entitlementId", concessao.entitlementId().toString()));

        return ResponseEntity.ok(ApiResponse.ok(WebhookEventAdminResponse.from(evento)));
    }

    private void validarIntervenivel(WebhookEvent evento) {
        if (!INTERVENIVEIS.contains(evento.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Evento em " + evento.getStatus() + " não aceita intervenção manual");
        }
    }
}
