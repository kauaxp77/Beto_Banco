package com.betobanco.leads.controller;

import com.betobanco.leads.entity.LeadMagnet;
import com.betobanco.leads.service.LeadService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Materiais de captacao. Documento Mestre Premium V3.0, secao 11.
 *
 * <p>Os quatro materiais que a secao nomeia sao semeados pela V18 <b>inativos</b>,
 * com URL de reserva. Esta rota e o que os coloca no ar: sem ela a captacao
 * inteira ficaria escrita e desligada.
 */
@RestController
@RequestMapping("/admin/lead-magnets")
@Tag(name = "Admin - Materiais de captação")
public class AdminLeadMagnetController {

    private final LeadService leads;

    public AdminLeadMagnetController(LeadService leads) {
        this.leads = leads;
    }

    @GetMapping
    @Operation(summary = "Todos os materiais, inclusive os inativos")
    public ResponseEntity<ApiResponse<List<MaterialAdminResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(leads.materiaisTodos().stream()
                .map(MaterialAdminResponse::de).toList()));
    }

    @PostMapping
    @Operation(summary = "Cadastra um material de captação")
    public ResponseEntity<ApiResponse<MaterialAdminResponse>> criar(
            @Valid @RequestBody NovoMaterialRequest req) {

        LeadMagnet criado = leads.criarMaterial(
                req.slug(), req.title(), req.kind(), req.fileUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(MaterialAdminResponse.de(criado)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza título, arquivo e disponibilidade do material")
    public ResponseEntity<ApiResponse<MaterialAdminResponse>> atualizar(
            @PathVariable("id") UUID id, @Valid @RequestBody EdicaoRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(MaterialAdminResponse.de(
                leads.atualizarMaterial(id, req.title(), req.fileUrl(), req.active()))));
    }

    // ------------------------------------------------------------------

    public record NovoMaterialRequest(
            @NotBlank(message = "Informe o identificador do material")
            @Pattern(regexp = "^[a-z0-9-]+$",
                     message = "Use apenas letras minúsculas, números e hífen")
            String slug,

            @NotBlank(message = "Informe o título")
            String title,

            @NotBlank(message = "Informe o tipo: PDF, MAPA_MENTAL, CRONOGRAMA ou QUESTOES")
            String kind,

            @NotBlank(message = "Informe o link do arquivo")
            String fileUrl) {
    }

    public record EdicaoRequest(
            @NotBlank(message = "Informe o título")
            String title,

            @NotBlank(message = "Informe o link do arquivo")
            String fileUrl,

            boolean active) {
    }

    public record MaterialAdminResponse(UUID id, String slug, String title, String kind,
                                        String fileUrl, boolean active) {

        static MaterialAdminResponse de(LeadMagnet m) {
            return new MaterialAdminResponse(m.getId(), m.getSlug(), m.getTitle(),
                    m.getKind(), m.getFileUrl(), m.isActive());
        }
    }
}
