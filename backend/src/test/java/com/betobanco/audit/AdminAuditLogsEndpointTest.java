package com.betobanco.audit;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.support.TestAuth;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminAuditLogsEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogger auditoria;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void adminListaAuditoriaComFiltroDeAcao() throws Exception {
        auditoria.registrar("ADMIN_ACTION", "Teste", "id-auditavel-1",
                Map.of("origem", "teste-audit-endpoint"));
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "admin-audit@teste.com", "ROLE_ADMIN");

        String json = mockMvc.perform(get("/admin/audit-logs")
                        .param("action", "ADMIN_ACTION")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andReturn().getResponse().getContentAsString();

        List<String> entidades = JsonPath.read(json, "$.data[*].entityId");
        assertThat(entidades).contains("id-auditavel-1");
        List<String> acoes = JsonPath.read(json, "$.data[*].action");
        assertThat(acoes).containsOnly("ADMIN_ACTION");
    }

    @Test
    void alunoNaoAcessaAuditoria() throws Exception {
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "aluno-audit@teste.com", "ROLE_STUDENT");

        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
