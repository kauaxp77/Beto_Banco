package com.betobanco.users;

import com.betobanco.audit.repository.AuditLogRepository;
import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminStudentsEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private UserDirectory diretorio;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private EntitlementService entitlements;

    @Autowired
    private AuditLogRepository auditoria;

    private String logarAdmin() throws Exception {
        String email = "admin-alunos@teste.com";
        if (users.findByEmailIgnoreCase(email).isEmpty()) {
            Role r = roles.findByName("ROLE_ADMIN").orElseThrow();
            User u = new User(email, "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"),
                    "Admin Alunos");
            u.getRoles().add(r);
            users.saveAndFlush(u);
        }
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }

    @Test
    void adminListaAlunosComBuscaEPaginacao() throws Exception {
        diretorio.registrar("busca-unica-xyz@aluno.com", "senha-forte-123", "Fulana Buscavel");
        String token = logarAdmin();

        String json = mockMvc.perform(get("/admin/students")
                        .param("search", "busca-unica-xyz")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andReturn().getResponse().getContentAsString();

        List<String> emails = JsonPath.read(json, "$.data[*].email");
        assertThat(emails).containsExactly("busca-unica-xyz@aluno.com");
    }

    @Test
    void adminVeDetalheDoAluno() throws Exception {
        var aluno = diretorio.registrar("detalhe@aluno.com", "senha-forte-123", "Aluno Detalhe");
        String token = logarAdmin();

        mockMvc.perform(get("/admin/students/" + aluno.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("detalhe@aluno.com"))
                .andExpect(jsonPath("$.data.fullName").value("Aluno Detalhe"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    void bloquearAlunoImpedeLoginEDesbloquearDevolve() throws Exception {
        diretorio.registrar("bloqueavel@aluno.com", "senha-forte-123", "Aluno Bloqueavel");
        var aluno = diretorio.buscarPorEmail("bloqueavel@aluno.com").orElseThrow();
        String token = logarAdmin();

        mockMvc.perform(patch("/admin/students/" + aluno.id() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        // Bloqueado nao entra.
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bloqueavel@aluno.com\","
                                + "\"password\":\"senha-forte-123\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(auditoria.findByActionOrderByCreatedAtDesc("STUDENT_BLOCKED")).isNotEmpty();

        mockMvc.perform(patch("/admin/students/" + aluno.id() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bloqueavel@aluno.com\","
                                + "\"password\":\"senha-forte-123\"}"))
                .andExpect(status().isOk());

        assertThat(auditoria.findByActionOrderByCreatedAtDesc("STUDENT_UNBLOCKED")).isNotEmpty();
    }

    @Test
    void statusInvalidoDevolve422() throws Exception {
        var aluno = diretorio.registrar("statusinvalido@aluno.com", "senha-forte-123", "Aluno X");
        String token = logarAdmin();

        mockMvc.perform(patch("/admin/students/" + aluno.id() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDURADO\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminConcedeERevogaEntitlementDiretamente() throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-ADM-ENT", "Mentoria Admin", null, 9900L)).getId();
        var aluno = diretorio.registrar("presenteado@aluno.com", "senha-forte-123",
                "Aluno Presenteado");
        String token = logarAdmin();

        String criado = mockMvc.perform(post("/admin/students/" + aluno.id() + "/entitlements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + produtoId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entitlementId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String eid = JsonPath.read(criado, "$.data.entitlementId");

        assertThat(entitlements.temAcesso(aluno.id(), produtoId)).isTrue();

        // A concessao manual fica registrada com o administrador responsavel.
        var admin = users.findByEmailIgnoreCase("admin-alunos@teste.com").orElseThrow();
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("ACCESS_GRANTED").stream()
                .anyMatch(a -> admin.getId().equals(a.getActorUserId()))).isTrue();

        mockMvc.perform(delete("/admin/students/" + aluno.id() + "/entitlements/" + eid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(entitlements.temAcesso(aluno.id(), produtoId)).isFalse();
        assertThat(auditoria.findByActionOrderByCreatedAtDesc("ACCESS_REVOKED").stream()
                .anyMatch(a -> admin.getId().equals(a.getActorUserId()))).isTrue();
    }

    @Test
    void concederProdutoInexistenteDevolve404() throws Exception {
        var aluno = diretorio.registrar("sempresente@aluno.com", "senha-forte-123", "Aluno Y");
        String token = logarAdmin();

        mockMvc.perform(post("/admin/students/" + aluno.id() + "/entitlements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoNaoAcessaGestaoDeAlunos() throws Exception {
        diretorio.registrar("intruso@aluno.com", "senha-forte-123", "Aluno Intruso");
        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"intruso@aluno.com\","
                                + "\"password\":\"senha-forte-123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.data.accessToken");

        mockMvc.perform(get("/admin/students")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
