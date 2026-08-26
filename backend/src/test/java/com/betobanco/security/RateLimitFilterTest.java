package com.betobanco.security;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Forca um limite baixo so nesta classe. O perfil de teste usa um limite alto
 * para nao interferir nas demais suites, que fazem dezenas de logins do mesmo
 * endereco.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "betobanco.auth.rate-limit-per-minute=5")
class RateLimitFilterTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitFilter filtro;

    private static final String CORPO =
            "{\"email\":\"forca@bruta.com\",\"password\":\"tentativa\"}";

    @Test
    void tentativasDemaisDeLoginDevolvem429NoEnvelope() throws Exception {
        for (int i = 0; i < filtro.limite(); i++) {
            mockMvc.perform(post("/auth/login")
                    .with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content(CORPO));
        }

        mockMvc.perform(post("/auth/login")
                        .with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.status").value(429));
    }

    @Test
    void outroIpNaoEhAfetadoPeloLimiteDoPrimeiro() throws Exception {
        for (int i = 0; i < filtro.limite() + 2; i++) {
            mockMvc.perform(post("/auth/login")
                    .with(r -> { r.setRemoteAddr("10.0.0.2"); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content(CORPO));
        }

        mockMvc.perform(post("/auth/login")
                        .with(r -> { r.setRemoteAddr("10.0.0.3"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotasQueNaoSaoDeAutenticacaoNaoSaoLimitadas() throws Exception {
        for (int i = 0; i < filtro.limite() + 5; i++) {
            mockMvc.perform(get("/actuator/health")
                    .with(r -> { r.setRemoteAddr("10.0.0.4"); return r; }));
        }

        mockMvc.perform(get("/actuator/health")
                        .with(r -> { r.setRemoteAddr("10.0.0.4"); return r; }))
                .andExpect(status().isOk());
    }
}
