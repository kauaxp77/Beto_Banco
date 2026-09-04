package com.betobanco.leads.controller;

import com.betobanco.leads.entity.LeadMagnet;
import com.betobanco.leads.service.LeadService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Captacao publica de leads. Documento Mestre Premium V3.0, secao 11.
 *
 * <p>Publico por definicao: o material existe para trocar conteudo pelo contato
 * de quem ainda nao tem conta. Atras de login ele nao captaria ninguem.
 *
 * <p>Por ser publico e de escrita, a rota entra na lista do
 * {@code RateLimitFilter}: sem limite, um script encheria o CRM de contatos
 * falsos e a lista de quem ligar deixaria de valer alguma coisa.
 */
@RestController
@RequestMapping("/leads")
@Tag(name = "Leads")
public class LeadController {

    private final LeadService leads;

    public LeadController(LeadService leads) {
        this.leads = leads;
    }

    @GetMapping("/magnets")
    @Operation(summary = "Materiais disponíveis para download mediante cadastro")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> materiais() {
        return ResponseEntity.ok(ApiResponse.ok(leads.materiaisDisponiveis().stream()
                .map(MaterialResponse::de).toList()));
    }

    /**
     * O caminho e {@code /leads/capture}, e nao {@code POST /leads}, por causa
     * do {@code RateLimitFilter}: ele casa a rota por sufixo da URI, e "/leads"
     * casaria tambem com "/admin/leads" — o CRM inteiro passaria a ser limitado
     * junto com o formulario publico.
     */
    @PostMapping("/capture")
    @Operation(summary = "Cadastra o contato e devolve o link do material")
    public ResponseEntity<ApiResponse<EntregaResponse>> capturar(
            @Valid @RequestBody CapturaRequest req) {

        LeadMagnet material = leads.capturar(
                req.name(), req.email(), req.whatsapp(), req.magnet());

        return ResponseEntity.ok(ApiResponse.ok(
                new EntregaResponse(material.getTitle(), material.getFileUrl())));
    }

    // ------------------------------------------------------------------

    public record CapturaRequest(
            @NotBlank(message = "Informe seu nome")
            @Size(max = 200)
            String name,

            @NotBlank(message = "Informe seu e-mail")
            @Email(message = "E-mail inválido")
            String email,

            /**
             * Opcional de proposito. A secao 11 quer o WhatsApp, mas exigi-lo
             * derruba a conversao do formulario — e um lead com e-mail vale
             * mais que um visitante que desistiu de preencher.
             */
            @Size(max = 30)
            String whatsapp,

            @NotBlank(message = "Informe qual material você quer")
            String magnet) {
    }

    /** Sem id do lead na resposta: o visitante nao precisa dele, e expo-lo daria a qualquer um uma chave para o CRM. */
    public record EntregaResponse(String title, String fileUrl) {
    }

    /** A URL do arquivo nao entra aqui: ela e a contrapartida do cadastro. */
    public record MaterialResponse(UUID id, String slug, String title, String kind) {

        static MaterialResponse de(LeadMagnet m) {
            return new MaterialResponse(m.getId(), m.getSlug(), m.getTitle(), m.getKind());
        }
    }
}
