package br.com.aprovacao.pagamento;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Secao 12 -- processamento do webhook fora do request HTTP.
 *
 * <p>Duas portas de entrada, uma so logica: {@link #agendar(UUID)} trata o evento
 * assim que ele chega, e {@link #varrerFila()} recolhe o que falhou ou o que ficou
 * para tras porque o processo caiu. A segunda e o que garante que nenhum pagamento
 * fique sem acesso quando a primeira falha -- o risco CRITICO-FINANCEIRO da secao 30.
 *
 * <p>O trabalho em si mora em {@link AplicadorDeEventoWebhook}, e nao aqui, porque
 * chamada de metodo no proprio bean nao atravessa o proxy do Spring: um
 * {@code @Transactional} invocado por {@code this} simplesmente nao abre transacao.
 */
@Service
public class ProcessadorWebhook {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorWebhook.class);

    private final WebhookEventoRepository eventos;
    private final AplicadorDeEventoWebhook aplicador;

    public ProcessadorWebhook(WebhookEventoRepository eventos, AplicadorDeEventoWebhook aplicador) {
        this.eventos = eventos;
        this.aplicador = aplicador;
    }

    @Async
    public void agendar(UUID eventoId) {
        try {
            aplicador.processar(eventoId);
        } catch (RuntimeException e) {
            // Nunca deixamos a excecao subir de uma thread assincrona: o registro da
            // falha e o backoff ja aconteceram dentro de processar().
            log.error("Falha ao processar evento {} de forma assincrona", eventoId, e);
        }
    }

    /**
     * Secao 12 -- varredura da fila com backoff. Um minuto de intervalo casa com o
     * menor passo do backoff (1 min); intervalo maior atrasaria a primeira
     * retentativa sem economizar nada.
     */
    @Scheduled(fixedDelay = 60_000)
    public void varrerFila() {
        List<WebhookEvento> pendentes = eventos.proximosDaFila(
                EnumSet.of(WebhookEvento.Status.RECEBIDO, WebhookEvento.Status.FALHA),
                Instant.now(),
                Limit.of(50));

        for (WebhookEvento evento : pendentes) {
            try {
                aplicador.processar(evento.getId());
            } catch (RuntimeException e) {
                log.error("Falha ao processar evento {} na varredura", evento.getId(), e);
            }
        }
    }
}
