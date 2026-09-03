package com.betobanco.essays.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Rubrica de correcao, por banca. Documento Mestre V4.0, secao 14.
 *
 * <p>"Configuravel por banca -- Cebraspe, FGV, FCC, Cesgranrio. Nota por
 * criterio, nao so total." Nota so total nao ensina nada: o aluno precisa saber
 * em qual criterio perdeu para saber o que treinar.
 *
 * <p>Os criterios ficam em JSONB, e nao em tabela filha, porque cada banca tem
 * um conjunto proprio que muda junto — e sempre lido inteiro, junto com a
 * rubrica, e nunca consultado isoladamente.
 */
@Entity
@Table(name = "essay_rubrics")
public class EssayRubric {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(nullable = false)
    private String board;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String criteria;

    @Column(nullable = false)
    private boolean active = true;

    protected EssayRubric() {
    }

    public EssayRubric(String board, String name, String criteria) {
        this.board = board;
        this.name = name;
        this.criteria = criteria;
    }

    public UUID getId() {
        return id;
    }

    public String getBoard() {
        return board;
    }

    public String getName() {
        return name;
    }

    /** JSON: [{"code","title","max_score"}]. */
    public String getCriteria() {
        return criteria;
    }

    public boolean isActive() {
        return active;
    }
}
