package com.betobanco.contests.service;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.contests.entity.Agency;
import com.betobanco.contests.entity.Career;
import com.betobanco.contests.entity.Contest;
import com.betobanco.contests.entity.ContestCareer;
import com.betobanco.contests.repository.AgencyRepository;
import com.betobanco.contests.repository.CareerRepository;
import com.betobanco.contests.repository.ContestCareerRepository;
import com.betobanco.contests.repository.ContestRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catalogo de concursos. Documento Mestre V4.0, secoes 07 e 11.
 */
@Service
public class ContestService {

    private static final Logger log = LoggerFactory.getLogger(ContestService.class);

    private final ContestRepository contests;
    private final ContestCareerRepository vinculos;
    private final CareerRepository careers;
    private final AgencyRepository agencies;
    private final AuditLogger auditoria;

    public ContestService(ContestRepository contests, ContestCareerRepository vinculos,
                          CareerRepository careers, AgencyRepository agencies,
                          AuditLogger auditoria) {
        this.contests = contests;
        this.vinculos = vinculos;
        this.careers = careers;
        this.agencies = agencies;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------
    // Publico
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Career> carreiras() {
        return careers.ativas(TenantContext.atual());
    }

    @Transactional(readOnly = true)
    public List<Agency> orgaos() {
        return agencies.todas(TenantContext.atual());
    }

    /**
     * Listagem com os filtros da secao 07.
     *
     * <p>Ordena por fim de inscricao ascendente: o concurso que fecha primeiro e
     * o que o aluno precisa ver primeiro. Ordenar por data de criacao poria em
     * cima o que acabou de ser cadastrado, que costuma ser o que ainda nem
     * abriu inscricao.
     */
    @Transactional(readOnly = true)
    public Page<Contest> listar(UUID careerId, UUID agencyId, String board, String status,
                                String educationLevel, Long salaryMin, Long salaryMax,
                                int pagina, int tamanho) {

        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100),
                Sort.by(Sort.Order.asc("registrationEnd").nullsLast(), Sort.Order.asc("name")));

        return contests.publicados(TenantContext.atual(), careerId, agencyId, board, status,
                educationLevel, salaryMin, salaryMax, paginacao);
    }

    /** A ficha completa e indexavel da secao 11. */
    @Transactional(readOnly = true)
    public Contest porSlug(String slug) {
        Contest contest = contests.findByTenantIdAndSlug(TenantContext.atual(), slug)
                .orElseThrow(() -> new NotFoundException("Concurso não encontrado"));

        if (!contest.publicado()) {
            // Ficha em preparo nao existe para o publico. 404, e nao 403: dizer
            // "existe mas voce nao pode ver" ja entrega que o concurso esta sendo
            // preparado, o que e informacao de negocio.
            throw new NotFoundException("Concurso não encontrado");
        }
        return contest;
    }

    @Transactional(readOnly = true)
    public List<Career> carreirasDo(UUID contestId) {
        List<UUID> ids = vinculos.findByContestId(contestId).stream()
                .map(ContestCareer::getCareerId).toList();
        return ids.isEmpty() ? List.of() : careers.findAllById(ids);
    }

    // ------------------------------------------------------------------
    // Admin
    // ------------------------------------------------------------------

    /**
     * Secao 11 — fila de revisao: ficha sem verificacao ha mais de 60 dias.
     *
     * <p>A fila existe porque o dado envelhece sozinho: um salario correto hoje
     * fica errado quando sai o reajuste, e ninguem e avisado. Sem a fila, a
     * ficha so e corrigida quando um aluno reclama — que e tarde demais para a
     * confianca que a secao 11 quer proteger.
     */
    @Transactional(readOnly = true)
    public List<Contest> filaDeRevisao(int limite) {
        Instant corte = Instant.now().minus(Contest.VALIDADE_DA_VERIFICACAO);
        return contests.paraRevisar(TenantContext.atual(), corte,
                PageRequest.of(0, Math.min(Math.max(limite, 1), 200)));
    }

    @Transactional
    public Contest registrarVerificacao(UUID contestId, UUID revisorId, String sourceUrl) {
        Contest contest = contests.findById(contestId)
                .orElseThrow(() -> new NotFoundException("Concurso não encontrado"));

        try {
            contest.registrarVerificacao(revisorId, sourceUrl);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }

        auditoria.registrar("CONTEST_VERIFIED", "Contest", contestId.toString(),
                Map.of("revisorId", revisorId.toString(), "sourceUrl", sourceUrl));
        log.info("Ficha {} verificada por {} contra {}.", contest.getSlug(), revisorId, sourceUrl);

        return contests.save(contest);
    }

    @Transactional
    public Contest publicar(UUID contestId, UUID adminId) {
        Contest contest = contests.findById(contestId)
                .orElseThrow(() -> new NotFoundException("Concurso não encontrado"));

        try {
            contest.publicar();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.CONFLICT, e.getMessage());
        }

        auditoria.registrar("CONTEST_PUBLISHED", "Contest", contestId.toString(),
                Map.of("adminId", adminId.toString()));
        return contests.save(contest);
    }

    /**
     * Define em quais carreiras o concurso aparece. Secao 07 — pode ser mais de
     * uma, e trocar o conjunto inteiro e mais simples e menos sujeito a sobra do
     * que calcular a diferenca.
     */
    @Transactional
    public void definirCarreiras(UUID contestId, List<UUID> careerIds) {
        contests.findById(contestId)
                .orElseThrow(() -> new NotFoundException("Concurso não encontrado"));

        vinculos.apagarDoContest(contestId);
        careerIds.stream().distinct()
                .map(careerId -> new ContestCareer(contestId, careerId))
                .forEach(vinculos::save);
    }
}
