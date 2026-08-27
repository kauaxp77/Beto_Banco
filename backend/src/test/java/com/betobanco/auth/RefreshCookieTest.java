package com.betobanco.auth;

import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 6.2: refresh token em cookie HttpOnly, nunca no corpo JSON. O access
 * token continua no corpo — ele vive so em memoria no frontend.
 */
@AutoConfigureMockMvc
class RefreshCookieTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDirectory usuarios;

    private MvcResult logar(String email) throws Exception {
        usuarios.registrar(email, "senha-forte-123", "Aluno Cookie");
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\"}"))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    void loginEmiteCookieHttpOnlyESemRefreshNoCorpo() throws Exception {
        MvcResult res = logar("cookie1@aluno.com");

        Cookie cookie = res.getResponse().getCookie("bb_refresh");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");

        // O valor do refresh nao pode aparecer no JSON: XSS que leia a
        // resposta nao pode roubar sessao de longa duracao.
        assertThat(res.getResponse().getContentAsString()).doesNotContain("refreshToken");
    }

    @Test
    void refreshUsaCookieERotaciona() throws Exception {
        Cookie cookie = logar("cookie2@aluno.com").getResponse().getCookie("bb_refresh");

        MvcResult renovado = mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        Cookie novo = renovado.getResponse().getCookie("bb_refresh");
        assertThat(novo).isNotNull();
        assertThat(novo.getValue()).isNotEqualTo(cookie.getValue());

        // O cookie antigo ja rotacionou: reusar e indicio de roubo -> 401.
        mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutLimpaOCookieERevogaASessao() throws Exception {
        Cookie cookie = logar("cookie3@aluno.com").getResponse().getCookie("bb_refresh");

        MvcResult saida = mockMvc.perform(post("/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent()).andReturn();
        Cookie limpo = saida.getResponse().getCookie("bb_refresh");
        assertThat(limpo).isNotNull();
        assertThat(limpo.getMaxAge()).isZero();

        mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshSemCookieDevolve401() throws Exception {
        mockMvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized());
    }
}
