package com.betobanco.dashboard;

import com.betobanco.catalog.entity.Product;
import com.betobanco.catalog.repository.ProductRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.repository.PaymentRepository;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.support.TestAuth;
import com.betobanco.users.api.UserDirectory;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O dashboard agrega numeros de varios modulos consumindo somente as
 * interfaces api/ deles — a regra ArchUnit reprova qualquer atalho. O banco e
 * compartilhado entre classes de teste, entao as assercoes sao pisos, nao
 * igualdades.
 */
@AutoConfigureMockMvc
class DashboardEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository produtos;

    @Autowired
    private PaymentRepository pagamentos;

    @Autowired
    private UserDirectory usuarios;

    @Autowired
    private EntitlementService entitlements;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void dashboardTrazOsNumerosDeTodosOsModulos() throws Exception {
        UUID produtoId = produtos.saveAndFlush(
                new Product("SKU-DASH", "Curso Dashboard", null, 111100L)).getId();
        var aluno = usuarios.registrar("aluno-dash@aluno.com", "senha-forte-123", "Aluno Dash");
        entitlements.conceder(aluno.id(), produtoId, "MANUAL", "teste-dashboard");
        Payment aprovado = new Payment("fake", "tx-dash", "aluno-dash@aluno.com",
                111100L, Payment.APPROVED);
        pagamentos.saveAndFlush(aprovado);

        String token = TestAuth.logarComo(mockMvc, users, roles,
                "admin-dash@teste.com", "ROLE_ADMIN");

        String json = mockMvc.perform(get("/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(((Number) JsonPath.read(json, "$.data.totalAlunos")).longValue())
                .isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(json, "$.data.produtosAtivos")).longValue())
                .isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(json, "$.data.entitlementsAtivos")).longValue())
                .isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(json, "$.data.pagamentosAprovados")).longValue())
                .isGreaterThanOrEqualTo(1);
        assertThat(((Number) JsonPath.read(json, "$.data.receitaAprovadaCents")).longValue())
                .isGreaterThanOrEqualTo(111100L);
        assertThat(((Number) JsonPath.read(json, "$.data.alunosBloqueados")).longValue())
                .isGreaterThanOrEqualTo(0);
        assertThat(((Number) JsonPath.read(json, "$.data.webhooksAguardandoAtencao")).longValue())
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void alunoNaoAcessaODashboard() throws Exception {
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "aluno-dash-403@teste.com", "ROLE_STUDENT");

        mockMvc.perform(get("/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
