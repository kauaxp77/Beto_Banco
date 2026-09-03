package br.com.aprovacao.pagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Secao 12 -- contrato do webhook.
 *
 * <p>"E a peca onde uma falha vira aluno pagando sem acesso, ou acesso sem
 * pagamento." Por isso o evento e gravado antes de qualquer processamento: se o
 * processo morrer no meio, o evento sobrevive e a fila o repete.
 */
@Entity
@Table(name = "webhook_evento")
public class WebhookEvento {

    public enum Status { RECEBIDO, PROCESSANDO, PROCESSADO, FALHA, FILA_MORTA, IGNORADO }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String gateway;

    @Column(name = "evento_id", nullable = false)
    private String eventoId;

    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false)
    private String payloadJson;

    @Column(name = "assinatura_ok", nullable = false)
    private boolean assinaturaOk;

    /** Momento no gateway. E o que ordena eventos, nao a hora em que chegaram aqui. */
    @Column(name = "ocorrido_em")
    private Instant ocorridoEm;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm = Instant.now();

    @Column(name = "processado_em")
    private Instant processadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RECEBIDO;

    @Column(nullable = false)
    private short tentativas;

    @Column(name = "proxima_em")
    private Instant proximaEm;

    @Column(length = 2000)
    private String erro;

    protected WebhookEvento() {}

    public WebhookEvento(String gateway, String eventoId, String tipo,
                         String payloadJson, boolean assinaturaOk, Instant ocorridoEm) {
        this.id = UUID.randomUUID();
        this.gateway = gateway;
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.payloadJson = payloadJson;
        this.assinaturaOk = assinaturaOk;
        this.ocorridoEm = ocorridoEm;
        this.proximaEm = Instant.now();
    }

    public void marcarProcessado() {
        this.status = Status.PROCESSADO;
        this.processadoEm = Instant.now();
        this.erro = null;
        this.proximaEm = null;
    }

    public void marcarIgnorado(String motivo) {
        this.status = Status.IGNORADO;
        this.processadoEm = Instant.now();
        this.erro = motivo;
        this.proximaEm = null;
    }

    /**
     * Secao 12: "Retentativa: falha de processamento entra em backoff 1 -> 5 -> 30
     * -> 120 min, e depois na fila morta com alerta."
     *
     * @return true quando o evento acabou de cair na fila morta -- o chamador usa
     *         isso para disparar o alerta uma unica vez.
     */
    public boolean registrarFalha(String mensagem, List<Integer> backoffMinutos) {
        this.tentativas++;
        this.erro = mensagem == null ? null : mensagem.substring(0, Math.min(mensagem.length(), 2000));

        if (tentativas > backoffMinutos.size()) {
            this.status = Status.FILA_MORTA;
            this.proximaEm = null;
            return true;
        }
        this.status = Status.FALHA;
        this.proximaEm = Instant.now().plusSeconds(backoffMinutos.get(tentativas - 1) * 60L);
        return false;
    }

    public UUID getId() { return id; }
    public String getGateway() { return gateway; }
    public String getEventoId() { return eventoId; }
    public String getTipo() { return tipo; }
    public String getPayloadJson() { return payloadJson; }
    public boolean isAssinaturaOk() { return assinaturaOk; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public Status getStatus() { return status; }
    public short getTentativas() { return tentativas; }
    public String getErro() { return erro; }
}
