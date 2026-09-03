package com.betobanco.essays.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma redacao enviada para correcao. Documento Mestre V4.0, secao 14.
 */
@Entity
@Table(name = "essays")
public class Essay {

    public static final String SUBMITTED = "SUBMITTED";
    public static final String IN_REVIEW = "IN_REVIEW";
    public static final String CORRECTED = "CORRECTED";
    public static final String REWRITE_SUBMITTED = "REWRITE_SUBMITTED";
    public static final String CANCELLED = "CANCELLED";

    /** Secao 14: prazo de 7 dias corridos, contagem visivel ao aluno. */
    public static final Duration PRAZO = Duration.ofDays(7);

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String prompt;

    @Column
    private String board;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "ocr_text")
    private String ocrText;

    @Column(nullable = false)
    private String status = SUBMITTED;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt = Instant.now().plus(PRAZO);

    @Column(name = "rewrite_of")
    private UUID rewriteOf;

    protected Essay() {
    }

    public Essay(UUID userId, String prompt, String board, String fileUrl) {
        this.userId = userId;
        this.prompt = prompt;
        this.board = board;
        this.fileUrl = fileUrl;
    }

    /** Reescrita (secao 14, passo 5): herda tema e banca da original. */
    public static Essay reescritaDe(Essay original, String fileUrl) {
        Essay nova = new Essay(original.userId, original.prompt, original.board, fileUrl);
        nova.rewriteOf = original.id;
        return nova;
    }

    public void marcarEmCorrecao() {
        this.status = IN_REVIEW;
    }

    public void marcarCorrigida() {
        this.status = CORRECTED;
    }

    /** Dias restantes do prazo. Negativo quando ja venceu — o corretor precisa ver isso. */
    public long diasRestantes() {
        return Duration.between(Instant.now(), dueAt).toDays();
    }

    public boolean aguardandoCorrecao() {
        return SUBMITTED.equals(status) || IN_REVIEW.equals(status);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getBoard() {
        return board;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public UUID getRewriteOf() {
        return rewriteOf;
    }
}
