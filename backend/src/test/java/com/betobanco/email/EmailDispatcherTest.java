package com.betobanco.email;

import com.betobanco.email.api.EmailService;
import com.betobanco.email.entity.EmailOutbox;
import com.betobanco.email.repository.EmailOutboxRepository;
import com.betobanco.email.service.EmailDispatcher;
import com.betobanco.email.service.EmailSender;
import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Testa o CICLO AGENDADO ({@code despachar()}), nao o processarLote() direto:
 * a query da fila usa lock pessimista, que exige transacao ativa — e uma
 * self-invocation nao passa pelo proxy do Spring. Chamar so o metodo interno
 * esconderia exatamente o bug que deixaria a outbox parada em producao.
 */
@TestPropertySource(properties = "betobanco.email.dispatcher-enabled=true")
class EmailDispatcherTest extends PostgresTestBase {

    @Autowired
    private EmailService emails;

    @Autowired
    private EmailDispatcher dispatcher;

    @Autowired
    private EmailOutboxRepository outbox;

    /** SMTP e a unica fronteira externa: mockar aqui e inevitavel. */
    @MockitoBean
    private EmailSender sender;

    @Test
    void oCicloAgendadoEnviaOQueEstaNaFila() {
        emails.enfileirar("agendado@aluno.com", EmailService.Templates.RECUPERACAO_SENHA,
                Map.of("nome", "Aluno Agendado", "token", "tok-agendado"),
                "teste-ciclo-agendado");

        dispatcher.despachar();

        EmailOutbox mensagem = outbox.findByDedupKey("teste-ciclo-agendado").orElseThrow();
        assertThat(mensagem.getStatus()).isEqualTo(EmailOutbox.SENT);
        verify(sender).enviar(eq("agendado@aluno.com"),
                eq(EmailService.Templates.RECUPERACAO_SENHA), anyMap());
    }
}
