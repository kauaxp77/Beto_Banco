package com.betobanco.payments;

import com.betobanco.payments.entity.Payment;
import com.betobanco.payments.repository.PaymentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminPaymentsEndpointTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository pagamentos;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void adminListaPagamentosComFiltroDeStatus() throws Exception {
        pagamentos.saveAndFlush(new Payment("fake", "tx-admin-pay-1", "pagante@aluno.com",
                5000L, Payment.APPROVED));
        pagamentos.saveAndFlush(new Payment("fake", "tx-admin-pay-2", "pendente@aluno.com",
                7000L, Payment.PENDING));
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "admin-pay@teste.com", "ROLE_ADMIN");

        String todos = mockMvc.perform(get("/admin/payments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andReturn().getResponse().getContentAsString();
        List<String> txs = JsonPath.read(todos, "$.data[*].providerTransactionId");
        assertThat(txs).contains("tx-admin-pay-1", "tx-admin-pay-2");

        String aprovados = mockMvc.perform(get("/admin/payments")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> txsAprovados = JsonPath.read(aprovados, "$.data[*].providerTransactionId");
        assertThat(txsAprovados).contains("tx-admin-pay-1").doesNotContain("tx-admin-pay-2");
    }

    @Test
    void alunoNaoAcessaPagamentos() throws Exception {
        String token = TestAuth.logarComo(mockMvc, users, roles,
                "aluno-pay@teste.com", "ROLE_STUDENT");

        mockMvc.perform(get("/admin/payments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
