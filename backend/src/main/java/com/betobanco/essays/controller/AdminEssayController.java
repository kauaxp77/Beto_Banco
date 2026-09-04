package com.betobanco.essays.controller;

import com.betobanco.essays.entity.EssayQuota;
import com.betobanco.essays.service.EssayQuotaService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Concessao de cota de redacao pelo admin. Documento Mestre V4.0, secao 14.
 *
 * <p>A cota nasce em zero e so cresce por tres caminhos que o
 * {@link EssayQuotaService} ja preve: a renovacao mensal da mentoria, a compra
 * avulsa e a concessao manual. As duas primeiras dependem do pagamento
 * (secao 12); esta e a terceira, e ate agora nao tinha rota.
 *
 * <p>Sem ela a secao 14 fica inalcancavel na pratica: o aluno com cota zero
 * recebe "sua cota acabou" no primeiro envio, e nao existe nenhum caminho no
 * produto para conceder a primeira.
 *
 * <p>Fica sob /admin, entao o SecurityConfig ja exige ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin/essays")
@Tag(name = "Admin - Redações")
public class AdminEssayController {

    private final EssayQuotaService cotas;

    public AdminEssayController(EssayQuotaService cotas) {
        this.cotas = cotas;
    }

    @GetMapping("/quota/{id}")
    @Operation(summary = "Cota de correções do aluno na competência atual")
    public ResponseEntity<ApiResponse<CotaResponse>> consultar(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(CotaResponse.de(cotas.cotaAtual(id))));
    }

    /**
     * O motivo e obrigatorio porque cota e dinheiro: cada correcao concedida
     * custa a hora de um corretor, e a secao 14 trata a cota como a regra que
     * mantem a margem de pe. Concessao sem motivo registrado vira um numero
     * que ninguem consegue explicar na conciliacao do mes seguinte.
     */
    @PostMapping("/quota/{id}")
    @Operation(summary = "Concede correções ao aluno na competência atual")
    public ResponseEntity<ApiResponse<CotaResponse>> conceder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ConcessaoRequest req) {

        EssayQuota cota = cotas.conceder(id, req.amount(), req.reason());
        return ResponseEntity.ok(ApiResponse.ok(CotaResponse.de(cota)));
    }

    // ------------------------------------------------------------------

    public record ConcessaoRequest(
            @Min(value = 1, message = "Conceda ao menos uma correção")
            @Max(value = 50, message = "Acima de 50 de uma vez, provavelmente é engano")
            int amount,

            @NotBlank(message = "Registre o motivo da concessão")
            String reason) {
    }

    public record CotaResponse(String competencia, int total, int usadas, int restantes) {

        static CotaResponse de(EssayQuota c) {
            return new CotaResponse(c.getPeriod().toString(), c.getAvailable(),
                    c.getUsed(), c.restantes());
        }
    }
}
