package com.betobanco.contests.entity;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A ficha de um concurso. Documento Mestre V4.0, secao 11.
 *
 * <p>"Cada concurso e uma pagina completa e indexavel." E, logo abaixo, o risco
 * que molda esta classe inteira: "Salario e vaga incorretos geram reclamacao e
 * perda de confianca. Toda ficha exibe 'verificado em DD/MM/AAAA' e link para a
 * fonte oficial."
 *
 * <p>Dai duas escolhas que nao sao cosmeticas: publicar exige {@code verifiedAt}
 * e {@code sourceUrl} (garantido por CHECK no banco, nao so aqui), e a ficha
 * sabe dizer sozinha se sua verificacao envelheceu.
 */
@Entity
@Table(name = "contests")
public class Contest {

    public static final String EXPECTED = "EXPECTED";
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String NOTICE_PUBLISHED = "NOTICE_PUBLISHED";
    public static final String REGISTRATION_OPEN = "REGISTRATION_OPEN";
    public static final String REGISTRATION_CLOSED = "REGISTRATION_CLOSED";
    public static final String EXAM_TAKEN = "EXAM_TAKEN";
    public static final String CLOSED = "CLOSED";

    /** Secao 11: "Ficha sem verificacao ha mais de 60 dias entra em fila de revisao". */
    public static final Duration VALIDADE_DA_VERIFICACAO = Duration.ofDays(60);

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = TenantContext.atual();

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column
    private String board;

    @Column(nullable = false)
    private String status = EXPECTED;

    @Column
    private Integer vacancies;

    @Column(name = "reserve_list")
    private Integer reserveList;

    @Column(name = "salary_cents")
    private Long salaryCents;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "weekly_hours")
    private Short weeklyHours;

    @Column
    private String benefits;

    @Column(name = "registration_start")
    private LocalDate registrationStart;

    @Column(name = "registration_end")
    private LocalDate registrationEnd;

    @Column(name = "registration_fee_cents")
    private Long registrationFeeCents;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "official_pdf_url")
    private String officialPdfUrl;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Contest() {
    }

    public Contest(UUID agencyId, String name, String slug) {
        this.agencyId = agencyId;
        this.name = name;
        this.slug = slug;
    }

    /**
     * Registra que uma pessoa conferiu a ficha contra a fonte oficial.
     *
     * <p>Exige a fonte junto: "verificado" sem dizer contra o que nao e
     * verificacao, e a §11 pede o link exatamente para que o aluno possa
     * conferir por conta propria o dado que vai decidir a inscricao dele.
     */
    public void registrarVerificacao(UUID revisorId, String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Verificação exige o link da fonte oficial contra a qual a ficha foi conferida.");
        }
        this.sourceUrl = sourceUrl;
        this.verifiedAt = Instant.now();
        this.verifiedBy = revisorId;
        this.updatedAt = Instant.now();
    }

    /** Secao 11 — a ficha so vai ao ar conferida e com fonte. */
    public void publicar() {
        if (verifiedAt == null || sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalStateException(
                    "Ficha sem verificação ou sem fonte oficial não pode ser publicada (seção 11).");
        }
        this.publishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void despublicar() {
        this.publishedAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean publicado() {
        return publishedAt != null;
    }

    /**
     * A verificacao envelheceu. Nunca verificada conta como defasada — e o caso
     * mais urgente da fila, nao o menos.
     */
    public boolean verificacaoDefasada() {
        return verifiedAt == null
                || verifiedAt.isBefore(Instant.now().minus(VALIDADE_DA_VERIFICACAO));
    }

    /** Dias desde a ultima verificacao; -1 quando nunca foi verificada. */
    public long diasDesdeVerificacao() {
        return verifiedAt == null ? -1 : Duration.between(verifiedAt, Instant.now()).toDays();
    }

    /** Inscricoes abertas hoje, pela data — nao pelo status, que alguem precisa lembrar de mudar. */
    public boolean inscricoesAbertas() {
        LocalDate hoje = LocalDate.now();
        return registrationStart != null && registrationEnd != null
                && !hoje.isBefore(registrationStart) && !hoje.isAfter(registrationEnd);
    }

    public UUID getId() { return id; }
    public UUID getAgencyId() { return agencyId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getBoard() { return board; }
    public String getStatus() { return status; }
    public Integer getVacancies() { return vacancies; }
    public Integer getReserveList() { return reserveList; }
    public Long getSalaryCents() { return salaryCents; }
    public String getEducationLevel() { return educationLevel; }
    public Short getWeeklyHours() { return weeklyHours; }
    public String getBenefits() { return benefits; }
    public LocalDate getRegistrationStart() { return registrationStart; }
    public LocalDate getRegistrationEnd() { return registrationEnd; }
    public Long getRegistrationFeeCents() { return registrationFeeCents; }
    public LocalDate getExamDate() { return examDate; }
    public String getOfficialPdfUrl() { return officialPdfUrl; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public void setBoard(String board) { this.board = board; }
    public void setStatus(String status) { this.status = status; }
    public void setVacancies(Integer v) { this.vacancies = v; }
    public void setReserveList(Integer r) { this.reserveList = r; }
    public void setSalaryCents(Long s) { this.salaryCents = s; }
    public void setEducationLevel(String e) { this.educationLevel = e; }
    public void setWeeklyHours(Short h) { this.weeklyHours = h; }
    public void setBenefits(String b) { this.benefits = b; }
    public void setRegistrationStart(LocalDate d) { this.registrationStart = d; }
    public void setRegistrationEnd(LocalDate d) { this.registrationEnd = d; }
    public void setRegistrationFeeCents(Long c) { this.registrationFeeCents = c; }
    public void setExamDate(LocalDate d) { this.examDate = d; }
    public void setOfficialPdfUrl(String u) { this.officialPdfUrl = u; }
}
