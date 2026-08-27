package com.betobanco.auth;

import com.betobanco.email.entity.EmailOutbox;
import com.betobanco.email.repository.EmailOutboxRepository;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O elo que faltava da Fase 2: forgot-password criava o token e o descartava.
 * Agora o token vai num e-mail de recuperacao enfileirado na outbox — nunca
 * enviado dentro da transacao.
 */
@AutoConfigureMockMvc
class ForgotPasswordOutboxTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDirectory usuarios;

    @Autowired
    private EmailOutboxRepository outbox;

    private void pedirRecuperacao(String email) throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isNoContent());
    }

    private List<EmailOutbox> emailsDe(String destinatario) {
        return outbox.findAll().stream()
                .filter(e -> destinatario.equals(e.getToAddress())
                        && "RECUPERACAO_SENHA".equals(e.getTemplate()))
                .toList();
    }

    @Test
    void forgotPasswordEnfileiraEmailComTokenUtilizavel() throws Exception {
        usuarios.registrar("esqueci@aluno.com", "senha-forte-123", "Aluna Esquecida");

        pedirRecuperacao("esqueci@aluno.com");

        List<EmailOutbox> emails = emailsDe("esqueci@aluno.com");
        assertThat(emails).hasSize(1);
        assertThat(emails.get(0).getStatus()).isEqualTo(EmailOutbox.PENDING);

        // O token do e-mail precisa funcionar de verdade no reset-password.
        String token = JsonPath.read(emails.get(0).getPayload(), "$.token");
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\","
                                + "\"password\":\"senha-nova-456\"}"))
                .andExpect(status().isNoContent());

        // E a senha nova passa a valer.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"esqueci@aluno.com\","
                                + "\"password\":\"senha-nova-456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void emailInexistenteRespondeIdenticoENaoEnfileiraNada() throws Exception {
        pedirRecuperacao("ninguem-por-aqui@aluno.com");

        assertThat(emailsDe("ninguem-por-aqui@aluno.com")).isEmpty();
    }

    @Test
    void doisPedidosGeramDoisEmailsComTokensDistintos() throws Exception {
        usuarios.registrar("insistente@aluno.com", "senha-forte-123", "Aluno Insistente");

        pedirRecuperacao("insistente@aluno.com");
        pedirRecuperacao("insistente@aluno.com");

        List<EmailOutbox> emails = emailsDe("insistente@aluno.com");
        assertThat(emails).hasSize(2);
        String token1 = JsonPath.read(emails.get(0).getPayload(), "$.token");
        String token2 = JsonPath.read(emails.get(1).getPayload(), "$.token");
        assertThat(token1).isNotEqualTo(token2);
    }
}
