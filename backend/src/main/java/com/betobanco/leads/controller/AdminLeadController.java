package com.betobanco.leads.controller;

import com.betobanco.leads.entity.Lead;
import com.betobanco.leads.entity.LeadEvent;
import com.betobanco.leads.service.LeadService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRM. Documento Mestre Premium V3.0, secoes 11 e 9 ("Leads. CRM.").
 *
 * <p>Fica sob /admin, entao o SecurityConfig ja exige ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin/leads")
@Tag(name = "Admin - Leads")
public class AdminLeadController {

    private final LeadService leads;

    public AdminLeadController(LeadService leads) {
        this.leads = leads;
    }

    @GetMapping
    @Operation(summary = "Fila do CRM, do contato mais recente para o mais antigo")
    public ResponseEntity<PageResponse<LeadResumo>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(PageResponse.from(
                leads.listar(status, search, page, size).map(LeadResumo::de)));
    }

    @GetMapping("/funnel")
    @Operation(summary = "Quantos leads em cada etapa do funil")
    public ResponseEntity<ApiResponse<Map<String, Long>>> funil() {
        return ResponseEntity.ok(ApiResponse.ok(leads.funil()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Um lead e todo o histórico de contatos dele")
    public ResponseEntity<ApiResponse<LeadDetalhe>> detalhe(@PathVariable("id") UUID id) {
        Lead lead = leads.porId(id);
        List<EventoResponse> historico = leads.historico(id).stream()
                .map(EventoResponse::de).toList();
        return ResponseEntity.ok(ApiResponse.ok(LeadDetalhe.de(lead, historico)));
    }

    @PostMapping("/{id}/status/{novo}")
    @Operation(summary = "Move o lead no funil")
    public ResponseEntity<ApiResponse<LeadResumo>> mudarStatus(
            @PathVariable("id") UUID id, @PathVariable("novo") String novo) {

        return ResponseEntity.ok(ApiResponse.ok(LeadResumo.de(leads.mudarStatus(id, novo))));
    }

    /**
     * Assume o lead. O dono e sempre quem chamou, tirado do token: aceitar um
     * id de dono no corpo permitiria atribuir trabalho a outra pessoa sem que
     * ela soubesse, e o CRM perderia o sentido de "meus leads".
     */
    @PostMapping("/{id}/claim")
    @Operation(summary = "Assume o lead para si")
    public ResponseEntity<ApiResponse<LeadResumo>> assumir(
            @AuthenticationPrincipal AuthenticatedUser atual, @PathVariable("id") UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(LeadResumo.de(leads.atribuir(id, atual.id()))));
    }

    @PutMapping("/{id}/notes")
    @Operation(summary = "Anota o que foi conversado")
    public ResponseEntity<ApiResponse<LeadResumo>> anotar(
            @PathVariable("id") UUID id, @Valid @RequestBody AnotacaoRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(LeadResumo.de(leads.anotar(id, req.notes()))));
    }

    // ------------------------------------------------------------------

    public record AnotacaoRequest(@Size(max = 4000) String notes) {
    }

    public record LeadResumo(UUID id, String name, String email, String whatsapp,
                             String status, UUID ownerId, Instant firstSeenAt,
                             Instant lastSeenAt) {

        static LeadResumo de(Lead l) {
            return new LeadResumo(l.getId(), l.getName(), l.getEmail(), l.getWhatsapp(),
                    l.getStatus(), l.getOwnerId(), l.getFirstSeenAt(), l.getLastSeenAt());
        }
    }

    public record LeadDetalhe(UUID id, String name, String email, String whatsapp,
                              String status, UUID ownerId, String notes,
                              Instant firstSeenAt, Instant lastSeenAt,
                              List<EventoResponse> history) {

        static LeadDetalhe de(Lead l, List<EventoResponse> historico) {
            return new LeadDetalhe(l.getId(), l.getName(), l.getEmail(), l.getWhatsapp(),
                    l.getStatus(), l.getOwnerId(), l.getNotes(),
                    l.getFirstSeenAt(), l.getLastSeenAt(), historico);
        }
    }

    public record EventoResponse(String source, UUID magnetId, UUID productId,
                                 Long amountCents, String reason, Instant occurredAt) {

        static EventoResponse de(LeadEvent e) {
            return new EventoResponse(e.getSource(), e.getMagnetId(), e.getProductId(),
                    e.getAmountCents(), e.getReason(), e.getOccurredAt());
        }
    }
}
