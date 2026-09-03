package com.betobanco.essays.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A devolutiva de uma redacao. Documento Mestre V4.0, secao 14.
 *
 * <p>"Anotacao sobre o texto, nota por criterio e comentario final em audio ou
 * texto."
 *
 * <p>O rascunho da IA fica em campo separado da nota, e nao misturado a ela, por
 * causa da regra da secao 14: "IA so sugere; nao publica nota". Separar em
 * colunas diferentes e o que torna impossivel um caminho de codigo publicar por
 * engano o que a IA escreveu como se fosse avaliacao do corretor.
 */
@Entity
@Table(name = "essay_corrections")
public class EssayCorrection {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "essay_id", nullable = false)
    private UUID essayId;

    @Column(name = "corrector_id", nullable = false)
    private UUID correctorId;

    @Column(name = "rubric_id")
    private UUID rubricId;

    /** {"C1": 160, "C2": 120} — nota por criterio. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String scores = "{}";

    @Column(name = "total_score")
    private BigDecimal totalScore;

    @Column
    private String comment;

    @Column(name = "audio_url")
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String annotations = "[]";

    /** Pre-analise da IA. Sugestao para o corretor; nunca vira nota sozinha. */
    @Column(name = "ai_draft")
    private String aiDraft;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected EssayCorrection() {
    }

    public EssayCorrection(UUID essayId, UUID correctorId, UUID rubricId) {
        this.essayId = essayId;
        this.correctorId = correctorId;
        this.rubricId = rubricId;
    }

    public void publicar(String scores, BigDecimal totalScore, String comment,
                         String audioUrl, String annotations) {
        this.scores = scores;
        this.totalScore = totalScore;
        this.comment = comment;
        this.audioUrl = audioUrl;
        this.annotations = annotations == null ? "[]" : annotations;
        this.completedAt = Instant.now();
    }

    public boolean publicada() {
        return completedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEssayId() {
        return essayId;
    }

    public UUID getCorrectorId() {
        return correctorId;
    }

    public UUID getRubricId() {
        return rubricId;
    }

    public String getScores() {
        return scores;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public String getComment() {
        return comment;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getAnnotations() {
        return annotations;
    }

    public String getAiDraft() {
        return aiDraft;
    }

    public void setAiDraft(String aiDraft) {
        this.aiDraft = aiDraft;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
