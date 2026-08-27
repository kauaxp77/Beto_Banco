package com.betobanco.dashboard.controller;

import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.dashboard.dto.DashboardResponse;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.payments.api.PaymentLedger;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.webhooks.api.WebhookMonitor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agregacao somente-leitura para o admin. Este modulo nao tem tabela propria
 * e consome exclusivamente as interfaces api/ dos demais — e o unico com essa
 * licenca (spec, secao 4).
 */
@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "Admin - Dashboard")
public class DashboardController {

    private final UserDirectory usuarios;
    private final ProductCatalog catalogo;
    private final EntitlementService entitlements;
    private final PaymentLedger pagamentos;
    private final WebhookMonitor webhooks;

    public DashboardController(UserDirectory usuarios, ProductCatalog catalogo,
                               EntitlementService entitlements, PaymentLedger pagamentos,
                               WebhookMonitor webhooks) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.entitlements = entitlements;
        this.pagamentos = pagamentos;
        this.webhooks = webhooks;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> resumo() {
        var alunos = usuarios.contarAlunos();
        var financeiro = pagamentos.resumo();

        return ResponseEntity.ok(ApiResponse.ok(new DashboardResponse(
                alunos.total(),
                alunos.bloqueados(),
                catalogo.contarAtivos(),
                entitlements.contarAtivos(),
                financeiro.aprovados(),
                financeiro.receitaAprovadaCents(),
                webhooks.aguardandoAtencao())));
    }
}
