package com.betobanco.webhooks;

import com.betobanco.audit.repository.AuditLogRepository;
import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.support.TestAuth;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.betobanco.webhooks.entity.WebhookEvent;
import com.betobanco.webhooks.repository.WebhookEventRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminWebhooksEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookEventRepository eventos;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private UserDirectory usuarios;

    @Autowired
    private EntitlementService entitlements;

    @Autowired
    private AuditLogRepository auditoria;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    private WebhookEvent eventoManual(String eventId) {
        WebhookEvent evento = new WebhookEvent("fake", eventId, "payment.approved", "{}");
        for (int i = 0; i < 5; i++) {
            evento.registrarFalha("SKU desconhecido: SKU-FANTASMA");
        }
        return eventos.saveAndFlush(evento);
    }

    private String logarAdmin() throws Exception {
        return TestAuth.logarComo(mockMvc, users, roles, "admin-wh@teste.com", "ROLE_ADMIN");
    }

    @Test
    void adminListaEventosFiltrandoPorStatus() throws Exception {
        eventoManual("evt-adm-lista");
        String token = logarAdmin();

        String manuais = mockMvc.perform(get("/admin/webhooks")
                        .param("status", "MANUAL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(manuais, "$.data[*].eventId");
        assertThat(ids).contains("evt-adm-lista");

        String processados = mockMvc.perform(get("/admin/webhooks")
                        .param("status", "PROCESSED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> idsProcessados = JsonPath.read(processados, "$.data[*].eventId");
        assertThat(idsProcessados).doesNotContain("evt-adm-lista");
    }

    @Test
    void reprocessarDevolveEventoParaAFila() throws Exception {
        UUID id = eventoManual("evt-adm-reproc").getId();
        String token = logarAdmin();

        mockMvc.perform(post("/admin/webhooks/" + id + "/reprocess")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));

        assertThat(eventos.findById(id).orElseThrow().getStatus())
                .isEqualTo(WebhookEvent.RECEIVED);
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("WEBHOOK_REPROCESSED")).isNotEmpty();
    }

    @Test
    void reprocessarEventoJaProcessadoDevolve409() throws Exception {
        WebhookEvent evento = new WebhookEvent("fake", "evt-adm-done", "payment.approved", "{}");
        evento.marcarProcessado();
        UUID id = eventos.saveAndFlush(evento).getId();
        String token = logarAdmin();

        mockMvc.perform(post("/admin/webhooks/" + id + "/reprocess")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void resolverManualmenteConcedeAcessoEEncerraOEvento() throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-RESOLVE", "Curso Resolvido", null, 900L)).getId();
        UUID id = eventoManual("evt-adm-resolve").getId();
        String token = logarAdmin();

        mockMvc.perform(post("/admin/webhooks/" + id + "/resolve-manually")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resolvido@aluno.com\","
                                + "\"productId\":\"" + produtoId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSED"));

        var aluno = usuarios.buscarPorEmail("resolvido@aluno.com").orElseThrow();
        assertThat(entitlements.temAcesso(aluno.id(), produtoId)).isTrue();
        assertThat(eventos.findById(id).orElseThrow().getStatus())
                .isEqualTo(WebhookEvent.PROCESSED);

        var admin = users.findByEmailIgnoreCase("admin-wh@teste.com").orElseThrow();
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("WEBHOOK_RESOLVED_MANUALLY").stream()
                .anyMatch(a -> admin.getId().equals(a.getActorUserId()))).isTrue();
    }

    @Test
    void alunoNaoAcessaWebhooks() throws Exception {
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "aluno-wh@teste.com", "ROLE_STUDENT");

        mockMvc.perform(get("/admin/webhooks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
