package com.betobanco.courses.service;

import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.courses.dto.CertificateResponse;
import com.betobanco.courses.dto.StudyStatsResponse;
import com.betobanco.courses.dto.TestimonialCreateRequest;
import com.betobanco.courses.dto.TestimonialResponse;
import com.betobanco.courses.dto.TrackResponse;
import com.betobanco.courses.entity.Certificate;
import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseModule;
import com.betobanco.courses.entity.CourseProduct;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonProgress;
import com.betobanco.courses.entity.Testimonial;
import com.betobanco.courses.repository.CertificateRepository;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.CourseProductRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.repository.LessonProgressRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.courses.repository.TestimonialRepository;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fase 4: o que mantem o aluno voltando — certificado, trilhas e constancia.
 * Nada aqui cria progresso; tudo deriva do que o aluno ja fez.
 */
@Service
public class StudentEngagementService {

    /** Sem 0/O/1/I: o codigo e digitado por terceiros ao validar. */
    private static final char[] ALFABETO_CODIGO =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final CourseProductRepository courseProducts;
    private final LessonProgressRepository progress;
    private final CertificateRepository certificates;
    private final TestimonialRepository testimonials;
    private final CourseAccess acesso;
    private final EntitlementService entitlements;
    private final UserDirectory usuarios;
    private final ProductCatalog catalogo;
    private final SecureRandom aleatorio = new SecureRandom();

    public StudentEngagementService(CourseRepository courses, CourseModuleRepository modules,
                                    LessonRepository lessons,
                                    CourseProductRepository courseProducts,
                                    LessonProgressRepository progress,
                                    CertificateRepository certificates,
                                    TestimonialRepository testimonials,
                                    CourseAccess acesso, EntitlementService entitlements,
                                    UserDirectory usuarios, ProductCatalog catalogo) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.courseProducts = courseProducts;
        this.progress = progress;
        this.certificates = certificates;
        this.testimonials = testimonials;
        this.acesso = acesso;
        this.entitlements = entitlements;
        this.usuarios = usuarios;
        this.catalogo = catalogo;
    }

    // ---------- certificados ----------

    /**
     * Emite (ou devolve, se ja emitido) o certificado do curso. Exige 100%
     * das aulas publicadas concluidas — do contrario, 409 com o que falta.
     */
    @Transactional
    public CertificateResponse emitirCertificado(UUID userId, UUID courseId) {
        Course curso = courses.findById(courseId)
                .filter(Course::isPublished)
                .filter(c -> acesso.cursosAcessiveis(userId).contains(c.getId()))
                .orElseThrow(() -> new NotFoundException("Curso não encontrado"));

        var existente = certificates.findByUserIdAndCourseId(userId, courseId);
        if (existente.isPresent()) {
            return responder(existente.get(), curso);
        }

        List<Lesson> aulas = aulasPublicadas(courseId);
        Set<UUID> concluidas = aulas.isEmpty() ? Set.of()
                : progress.findByUserIdAndLessonIdIn(userId,
                                aulas.stream().map(Lesson::getId).toList()).stream()
                        .map(LessonProgress::getLessonId)
                        .collect(Collectors.toSet());
        long faltam = aulas.stream().filter(a -> !concluidas.contains(a.getId())).count();
        if (aulas.isEmpty() || faltam > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Conclua todas as aulas para emitir o certificado ("
                            + faltam + " restante" + (faltam == 1 ? "" : "s") + ")");
        }

        int horas = horasDoCurso(aulas);
        try {
            Certificate emitido = certificates.saveAndFlush(
                    new Certificate(userId, courseId, gerarCodigo(), horas));
            return responder(emitido, curso);
        } catch (DataIntegrityViolationException e) {
            // Corrida entre abas: a unique (user, course) garantiu um so.
            return certificates.findByUserIdAndCourseId(userId, courseId)
                    .map(c -> responder(c, curso))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> meusCertificados(UUID userId) {
        return certificates.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(c -> responder(c, courses.findById(c.getCourseId()).orElse(null)))
                .toList();
    }

    /** Validacao publica: quem tem o codigo ve o certificado, mais nada. */
    @Transactional(readOnly = true)
    public CertificateResponse validar(String code) {
        Certificate certificado = certificates
                .findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Certificado não encontrado"));
        return responder(certificado, courses.findById(certificado.getCourseId()).orElse(null));
    }

    private CertificateResponse responder(Certificate c, Course curso) {
        String nome = usuarios.buscarAtivoPorId(c.getUserId())
                .map(UserAccount::fullName)
                .orElse("Aluno");
        return new CertificateResponse(c.getCode(), nome, c.getCourseId(),
                curso == null ? null : curso.getTitle(), c.getHours(), c.getIssuedAt());
    }

    private String gerarCodigo() {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            StringBuilder sb = new StringBuilder("BB-");
            for (int i = 0; i < 10; i++) {
                if (i == 5) {
                    sb.append('-');
                }
                sb.append(ALFABETO_CODIGO[aleatorio.nextInt(ALFABETO_CODIGO.length)]);
            }
            String codigo = sb.toString();
            if (certificates.findByCode(codigo).isEmpty()) {
                return codigo;
            }
        }
        throw new IllegalStateException("nao foi possivel gerar codigo unico de certificado");
    }

    private List<Lesson> aulasPublicadas(UUID courseId) {
        List<CourseModule> modulosDoCurso =
                modules.findByCourseIdOrderByPositionAscTitleAsc(courseId);
        if (modulosDoCurso.isEmpty()) {
            return List.of();
        }
        return lessons.findByModuleIdInOrderByPositionAscTitleAsc(
                        modulosDoCurso.stream().map(CourseModule::getId).toList()).stream()
                .filter(Lesson::isPublished)
                .toList();
    }

    private static int horasDoCurso(List<Lesson> aulas) {
        long segundos = aulas.stream()
                .mapToLong(a -> a.getDurationSeconds() == null ? 0 : a.getDurationSeconds())
                .sum();
        return (int) Math.max(1, Math.ceil(segundos / 3600.0));
    }

    // ---------- constancia (streak) ----------

    @Transactional(readOnly = true)
    public StudyStatsResponse estatisticas(UUID userId) {
        TreeSet<LocalDate> dias = progress.findByUserId(userId).stream()
                .map(p -> p.getCompletedAt().atZone(FUSO).toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        LocalDate hoje = LocalDate.now(FUSO);
        boolean estudouHoje = dias.contains(hoje);

        // Streak atual: conta para tras a partir de hoje (ou de ontem, para
        // nao zerar o contador de quem ainda nao estudou hoje).
        int atual = 0;
        LocalDate cursor = estudouHoje ? hoje : hoje.minusDays(1);
        while (dias.contains(cursor)) {
            atual++;
            cursor = cursor.minusDays(1);
        }

        int melhor = 0;
        int corrente = 0;
        LocalDate anterior = null;
        for (LocalDate dia : dias) {
            corrente = (anterior != null && anterior.plusDays(1).equals(dia)) ? corrente + 1 : 1;
            melhor = Math.max(melhor, corrente);
            anterior = dia;
        }

        int ativos30 = (int) dias.stream()
                .filter(d -> !d.isBefore(hoje.minusDays(29)))
                .count();

        return new StudyStatsResponse(atual, melhor, ativos30, estudouHoje);
    }

    // ---------- trilhas (combos) ----------

    /** Produtos do aluno que liberam 2+ cursos viram trilhas com progresso agregado. */
    @Transactional(readOnly = true)
    public List<TrackResponse> minhasTrilhas(UUID userId) {
        List<UUID> produtosDoAluno = entitlements.listarDe(userId).stream()
                .map(EntitlementService.Item::productId)
                .toList();
        if (produtosDoAluno.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<UUID>> cursosPorProduto =
                courseProducts.findByProductIdIn(produtosDoAluno).stream()
                        .collect(Collectors.groupingBy(CourseProduct::getProductId,
                                Collectors.mapping(CourseProduct::getCourseId,
                                        Collectors.toList())));

        return cursosPorProduto.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)
                .map(e -> montarTrilha(userId, e.getKey(), e.getValue()))
                .filter(t -> t != null && !t.courses().isEmpty())
                .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
                .toList();
    }

    private TrackResponse montarTrilha(UUID userId, UUID productId, List<UUID> courseIds) {
        var produto = catalogo.buscarPorId(productId).orElse(null);
        if (produto == null) {
            return null;
        }

        List<TrackResponse.TrackCourse> cursosDaTrilha =
                courses.findByIdInAndPublishedTrueOrderByTitle(courseIds).stream()
                        .map(curso -> {
                            List<Lesson> aulas = aulasPublicadas(curso.getId());
                            long feitas = aulas.isEmpty() ? 0
                                    : progress.findByUserIdAndLessonIdIn(userId,
                                            aulas.stream().map(Lesson::getId).toList()).size();
                            return new TrackResponse.TrackCourse(curso.getId(),
                                    curso.getTitle(), aulas.size(), feitas);
                        })
                        .toList();

        long total = cursosDaTrilha.stream()
                .mapToLong(TrackResponse.TrackCourse::totalLessons).sum();
        long feitas = cursosDaTrilha.stream()
                .mapToLong(TrackResponse.TrackCourse::completedLessons).sum();

        return new TrackResponse(productId, produto.name(), total, feitas, cursosDaTrilha);
    }

    // ---------- depoimentos ----------

    @Transactional
    public TestimonialResponse enviarDepoimento(UUID userId, TestimonialCreateRequest req) {
        if (req.courseId() != null
                && !acesso.cursosAcessiveis(userId).contains(req.courseId())) {
            throw new NotFoundException("Curso não encontrado");
        }
        Testimonial criado = testimonials.saveAndFlush(
                new Testimonial(userId, req.courseId(), req.body().trim()));
        return responderDepoimento(criado);
    }

    @Transactional(readOnly = true)
    public List<TestimonialResponse> meusDepoimentos(UUID userId) {
        return testimonials.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::responderDepoimento)
                .toList();
    }

    /** Prova social publica: apenas aprovados, sem e-mail do autor. */
    @Transactional(readOnly = true)
    public List<TestimonialResponse> aprovados() {
        return testimonials.findByStatusOrderByCreatedAtDesc(Testimonial.APPROVED).stream()
                .limit(30)
                .map(t -> {
                    TestimonialResponse r = responderDepoimento(t);
                    return new TestimonialResponse(r.id(), r.authorName(), null, r.courseId(),
                            r.courseTitle(), r.body(), r.status(), r.createdAt());
                })
                .toList();
    }

    public TestimonialResponse responderDepoimento(Testimonial t) {
        var autor = usuarios.buscarAtivoPorId(t.getUserId());
        String tituloCurso = t.getCourseId() == null ? null
                : courses.findById(t.getCourseId()).map(Course::getTitle).orElse(null);
        return new TestimonialResponse(t.getId(),
                autor.map(UserAccount::fullName).orElse("Aluno"),
                autor.map(UserAccount::email).orElse(null),
                t.getCourseId(), tituloCurso, t.getBody(), t.getStatus(), t.getCreatedAt());
    }
}
