package br.com.aprovacao.conteudo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

/**
 * Secao 21: "Log imutavel de acao administrativa, retido por 5 anos."
 * Secao 20: "Toda acao de Admin e Suporte grava em log_auditoria."
 *
 * <p>A entidade nao expoe nenhum setter de proposito: uma linha de auditoria que
 * pode ser editada nao e auditoria. Correcao, quando necessaria, e uma linha nova.
 */
@Entity
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private String acao;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id")
    private String entidadeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_json", columnDefinition = "jsonb", nullable = false)
    private String dadosJson = "{}";

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected LogAuditoria() {}

    public LogAuditoria(UUID tenantId, UUID usuarioId, String acao, String entidade,
                        String entidadeId, String dadosJson, String ip, String userAgent) {
        this.tenantId = tenantId;
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.dadosJson = dadosJson == null ? "{}" : dadosJson;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public Long getId() { return id; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public String getEntidadeId() { return entidadeId; }
    public Instant getCriadoEm() { return criadoEm; }
}
