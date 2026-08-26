package com.betobanco.security;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwt;

    @Test
    void rotaProtegidaSemTokenDevolve401NoEnvelopePadrao() throws Exception {
        mockMvc.perform(get("/students/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.status").value(401))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void alunoNaoAcessaRotaAdministrativaERecebe403NoEnvelope() throws Exception {
        String token = jwt.gerar(UUID.randomUUID(), "aluno@exemplo.com", Set.of("ROLE_STUDENT"));

        mockMvc.perform(get("/admin/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.status").value(403));
    }

    @Test
    void tokenInvalidoNaoAutentica() throws Exception {
        mockMvc.perform(get("/students/me").header("Authorization", "Bearer lixo.nao.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void rotasPublicasSeguemAbertas() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
