package com.betobanco.contests.controller;

import com.betobanco.contests.entity.Agency;
import com.betobanco.contests.entity.Career;
import com.betobanco.contests.entity.Contest;
import com.betobanco.contests.service.ContestService;
import com.betobanco.contests.service.SearchService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catalogo publico de concursos e busca. Documento Mestre V4.0, secoes 07 e 11.
 *
 * <p>Publico porque a secao 15 conta com estas paginas como fonte de trafego
 * organico — uma ficha de concurso atras de login nao e indexavel, e a secao 11
 * a chama de "pagina completa e indexavel".
 */
@RestController
@RequestMapping("/contests")
@Tag(name = "Concursos")
public class ContestController {

    private final ContestService contests;
    private final SearchService busca;

    public ContestController(ContestService contests, SearchService busca) {
        this.contests = contests;
        this.busca = busca;
    }

    @GetMapping("/careers")
    @Operation(summary = "Carreiras ativas, na ordem definida pelo admin")
    public ResponseEntity<ApiResponse<List<CarreiraResponse>>> carreiras() {
        return ResponseEntity.ok(ApiResponse.ok(contests.carreiras().stream()
                .map(c -> new CarreiraResponse(c.getId(), c.getName(), c.getSlug(),
                        c.getDescription(), c.getPosition()))
                .toList()));
    }

    @GetMapping("/agencies")
    @Operation(summary = "Órgãos cadastrados")
    public ResponseEntity<ApiResponse<List<OrgaoResponse>>> orgaos() {
        return ResponseEntity.ok(ApiResponse.ok(contests.orgaos().stream()
                .map(a -> new OrgaoResponse(a.getId(), a.getName(), a.getAcronym(),
                        a.getSphere(), a.getState(), a.getSiteUrl(), a.getLogoUrl()))
                .toList()));
    }

    @GetMapping
    @Operation(summary = "Lista concursos publicados, com os filtros da seção 07")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listar(
            @RequestParam(required = false) UUID career,
            @RequestParam(required = false) UUID agency,
            @RequestParam(required = false) String board,
            @RequestParam(required = false) String status,
            @RequestParam(name = "education_level", required = false) String educationLevel,
            @RequestParam(name = "salary_min", required = false) Long salaryMin,
            @RequestParam(name = "salary_max", required = false) Long salaryMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Contest> pagina = contests.listar(career, agency, board, status,
                educationLevel, salaryMin, salaryMax, page, size);

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("items", pagina.getContent().stream().map(ResumoResponse::de).toList());
        corpo.put("page", pagina.getNumber());
        corpo.put("totalPages", pagina.getTotalPages());
        corpo.put("totalItems", pagina.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(corpo));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Ficha completa e indexável do concurso")
    public ResponseEntity<ApiResponse<FichaResponse>> ficha(@PathVariable String slug) {
        Contest contest = contests.porSlug(slug);
        List<String> carreiras = contests.carreirasDo(contest.getId()).stream()
                .map(Career::getName).toList();
        return ResponseEntity.ok(ApiResponse.ok(FichaResponse.de(contest, carreiras)));
    }

    /** Secao 07 — busca unica sobre concursos e cursos. */
    @GetMapping("/search")
    @Operation(summary = "Busca unificada, tolerante a acento e erro de digitação")
    public ResponseEntity<ApiResponse<List<SearchService.Resultado>>> buscar(
            @RequestParam("q") String termo,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) UUID career,
            @RequestParam(required = false) UUID agency,
            @RequestParam(required = false) String board,
            @RequestParam(name = "education_level", required = false) String educationLevel,
            @RequestParam(name = "salary_min", required = false) Long salaryMin,
            @RequestParam(name = "salary_max", required = false) Long salaryMax,
            @RequestParam(required = false) String status,
            @RequestParam(name = "sort", defaultValue = "relevancia") String ordenarPor,
            @RequestParam(defaultValue = "20") int limit) {

        SearchService.Filtros filtros = new SearchService.Filtros(
                kind, career, agency, board, educationLevel, salaryMin, salaryMax, status);

        return ResponseEntity.ok(ApiResponse.ok(busca.buscar(termo, filtros, ordenarPor, limit)));
    }

    // ------------------------------------------------------------------

    public record CarreiraResponse(UUID id, String name, String slug, String description,
                                   int position) {
    }

    public record OrgaoResponse(UUID id, String name, String acronym, String sphere,
                                String state, String siteUrl, String logoUrl) {
    }

    public record ResumoResponse(UUID id, String name, String slug, String board, String status,
                                 Integer vacancies, Long salaryCents, LocalDate registrationEnd,
                                 LocalDate examDate, boolean registrationOpen,
                                 Instant verifiedAt, boolean verificationStale) {

        static ResumoResponse de(Contest c) {
            return new ResumoResponse(c.getId(), c.getName(), c.getSlug(), c.getBoard(),
                    c.getStatus(), c.getVacancies(), c.getSalaryCents(), c.getRegistrationEnd(),
                    c.getExamDate(), c.inscricoesAbertas(), c.getVerifiedAt(),
                    c.verificacaoDefasada());
        }
    }

    /**
     * A ficha completa.
     *
     * <p>{@code verifiedAt}, {@code sourceUrl} e {@code verificationStale} saem
     * na resposta de proposito: a secao 11 manda a pagina exibir "verificado em
     * DD/MM/AAAA" e o link da fonte. Quando a verificacao envelheceu, o aluno
     * precisa saber disso antes de decidir a inscricao com base no salario que
     * esta escrito ali.
     */
    public record FichaResponse(UUID id, String name, String slug, String board, String status,
                                Integer vacancies, Integer reserveList, Long salaryCents,
                                String educationLevel, Short weeklyHours, String benefits,
                                LocalDate registrationStart, LocalDate registrationEnd,
                                Long registrationFeeCents, LocalDate examDate,
                                boolean registrationOpen, String officialPdfUrl, String sourceUrl,
                                Instant verifiedAt, boolean verificationStale,
                                long daysSinceVerification, List<String> careers) {

        static FichaResponse de(Contest c, List<String> carreiras) {
            return new FichaResponse(c.getId(), c.getName(), c.getSlug(), c.getBoard(),
                    c.getStatus(), c.getVacancies(), c.getReserveList(), c.getSalaryCents(),
                    c.getEducationLevel(), c.getWeeklyHours(), c.getBenefits(),
                    c.getRegistrationStart(), c.getRegistrationEnd(), c.getRegistrationFeeCents(),
                    c.getExamDate(), c.inscricoesAbertas(), c.getOfficialPdfUrl(), c.getSourceUrl(),
                    c.getVerifiedAt(), c.verificacaoDefasada(), c.diasDesdeVerificacao(),
                    carreiras);
        }
    }
}
