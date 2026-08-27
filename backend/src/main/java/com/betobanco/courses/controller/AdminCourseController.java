package com.betobanco.courses.controller;

import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.courses.dto.CourseAdminResponse;
import com.betobanco.courses.dto.CourseContentResponse;
import com.betobanco.courses.dto.CourseCreateRequest;
import com.betobanco.courses.dto.CourseUpdateRequest;
import com.betobanco.courses.dto.LessonRequest;
import com.betobanco.courses.dto.LinkProductRequest;
import com.betobanco.courses.dto.MaterialRequest;
import com.betobanco.courses.dto.ModuleRequest;
import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseModule;
import com.betobanco.courses.entity.CourseProduct;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonMaterial;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.CourseProductRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.entity.QuizQuestion;
import com.betobanco.courses.repository.LessonMaterialRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.courses.repository.QuizQuestionRepository;
import com.betobanco.courses.service.AdminCourseLookup;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestao de conteudo. Diferente do catalogo, aqui DELETE existe: modulo e
 * aula sao conteudo editorial, nao registro financeiro — apagar cascateia
 * apenas progresso, nunca pagamento.
 */
@RestController
@RequestMapping("/admin/courses")
@Tag(name = "Admin - Courses")
public class AdminCourseController {

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final CourseProductRepository courseProducts;
    private final LessonMaterialRepository materials;
    private final QuizQuestionRepository quizQuestions;
    private final ProductCatalog catalogo;
    private final AdminCourseLookup busca;

    public AdminCourseController(CourseRepository courses, CourseModuleRepository modules,
                                 LessonRepository lessons,
                                 CourseProductRepository courseProducts,
                                 LessonMaterialRepository materials,
                                 QuizQuestionRepository quizQuestions,
                                 ProductCatalog catalogo, AdminCourseLookup busca) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.courseProducts = courseProducts;
        this.materials = materials;
        this.quizQuestions = quizQuestions;
        this.catalogo = catalogo;
        this.busca = busca;
    }

    // ---------- cursos ----------

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CourseAdminResponse>>> listar() {
        List<CourseAdminResponse> todos = courses.findAll(Sort.by("title")).stream()
                .map(this::responder)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(todos));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<CourseAdminResponse>> detalhar(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(responder(busca.curso(id))));
    }

    /** Arvore completa de modulos e aulas, incluindo nao publicadas. */
    @GetMapping("/{id}/content")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<CourseContentResponse>> conteudo(
            @PathVariable("id") UUID id) {
        Course curso = busca.curso(id);
        List<CourseModule> modulosDoCurso =
                modules.findByCourseIdOrderByPositionAscTitleAsc(curso.getId());
        List<Lesson> todasAulas = modulosDoCurso.isEmpty() ? List.<Lesson>of()
                : lessons.findByModuleIdInOrderByPositionAscTitleAsc(
                        modulosDoCurso.stream().map(CourseModule::getId).toList());

        Map<UUID, List<LessonMaterial>> materiaisPorAula = todasAulas.isEmpty() ? Map.of()
                : materials.findByLessonIdInOrderByPositionAscTitleAsc(
                                todasAulas.stream().map(Lesson::getId).toList()).stream()
                        .collect(Collectors.groupingBy(LessonMaterial::getLessonId));

        Map<UUID, Long> questoesPorAula = todasAulas.isEmpty() ? Map.of()
                : quizQuestions.findByLessonIdIn(
                                todasAulas.stream().map(Lesson::getId).toList()).stream()
                        .collect(Collectors.groupingBy(QuizQuestion::getLessonId,
                                Collectors.counting()));

        List<CourseContentResponse.ModuleContent> arvore = modulosDoCurso.stream()
                .map(m -> new CourseContentResponse.ModuleContent(m.getId(), m.getTitle(),
                        m.getPosition(),
                        todasAulas.stream()
                                .filter(a -> a.getModuleId().equals(m.getId()))
                                .map(a -> new CourseContentResponse.LessonContent(a.getId(),
                                        a.getTitle(), a.getDescription(), a.getVideoUrl(),
                                        a.getDurationSeconds(), a.getPosition(),
                                        a.isPublished(),
                                        materiaisPorAula.getOrDefault(a.getId(), List.of())
                                                .stream()
                                                .map(mat -> new CourseContentResponse
                                                        .MaterialContent(mat.getId(),
                                                                mat.getTitle(), mat.getUrl()))
                                                .toList(),
                                        questoesPorAula.getOrDefault(a.getId(), 0L)))
                                .toList()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                new CourseContentResponse(curso.getId(), curso.getTitle(), arvore)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseAdminResponse>> criar(
            @Valid @RequestBody CourseCreateRequest req) {
        String slug = slugDe(req.title());
        if (courses.findBySlug(slug).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Já existe curso com este título");
        }
        try {
            Course criado = courses.saveAndFlush(new Course(req.title().trim(), slug,
                    req.description(), req.coverUrl()));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(responder(criado)));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "Já existe curso com este título");
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<CourseAdminResponse>> atualizar(
            @PathVariable("id") UUID id, @Valid @RequestBody CourseUpdateRequest req) {
        Course curso = busca.curso(id);
        curso.setTitle(req.title().trim());
        curso.setDescription(req.description());
        curso.setCoverUrl(req.coverUrl());
        curso.setPublished(req.published());
        courses.saveAndFlush(curso);
        return ResponseEntity.ok(ApiResponse.ok(responder(curso)));
    }

    // ---------- vinculo com produtos ----------

    @PostMapping("/{id}/products")
    @Transactional
    public ResponseEntity<ApiResponse<CourseAdminResponse>> vincularProduto(
            @PathVariable("id") UUID id, @Valid @RequestBody LinkProductRequest req) {
        Course curso = busca.curso(id);
        catalogo.buscarPorId(req.productId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        if (courseProducts.findByCourseIdAndProductId(curso.getId(), req.productId()).isEmpty()) {
            courseProducts.saveAndFlush(new CourseProduct(curso.getId(), req.productId()));
        }
        return ResponseEntity.ok(ApiResponse.ok(responder(curso)));
    }

    @DeleteMapping("/{id}/products/{productId}")
    @Transactional
    public ResponseEntity<ApiResponse<CourseAdminResponse>> desvincularProduto(
            @PathVariable("id") UUID id, @PathVariable("productId") UUID productId) {
        Course curso = busca.curso(id);
        courseProducts.findByCourseIdAndProductId(curso.getId(), productId)
                .ifPresent(courseProducts::delete);
        return ResponseEntity.ok(ApiResponse.ok(responder(curso)));
    }

    // ---------- modulos ----------

    @PostMapping("/{id}/modules")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> criarModulo(
            @PathVariable("id") UUID id, @Valid @RequestBody ModuleRequest req) {
        Course curso = busca.curso(id);
        modules.saveAndFlush(new CourseModule(curso.getId(), req.title().trim(), req.position()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @PutMapping("/modules/{moduleId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> atualizarModulo(
            @PathVariable("moduleId") UUID moduleId, @Valid @RequestBody ModuleRequest req) {
        CourseModule modulo = busca.modulo(moduleId);
        modulo.setTitle(req.title().trim());
        modulo.setPosition(req.position());
        modules.saveAndFlush(modulo);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/modules/{moduleId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removerModulo(
            @PathVariable("moduleId") UUID moduleId) {
        modules.delete(busca.modulo(moduleId));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---------- aulas ----------

    @PostMapping("/modules/{moduleId}/lessons")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> criarAula(
            @PathVariable("moduleId") UUID moduleId, @Valid @RequestBody LessonRequest req) {
        CourseModule modulo = busca.modulo(moduleId);
        Lesson aula = new Lesson(modulo.getId(), req.title().trim(), req.description(),
                req.videoUrl(), req.durationSeconds(), req.position());
        if (req.published() != null) {
            aula.setPublished(req.published());
        }
        lessons.saveAndFlush(aula);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @PutMapping("/lessons/{lessonId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> atualizarAula(
            @PathVariable("lessonId") UUID lessonId, @Valid @RequestBody LessonRequest req) {
        Lesson aula = busca.aula(lessonId);
        aula.setTitle(req.title().trim());
        aula.setDescription(req.description());
        aula.setVideoUrl(req.videoUrl());
        aula.setDurationSeconds(req.durationSeconds());
        aula.setPosition(req.position());
        if (req.published() != null) {
            aula.setPublished(req.published());
        }
        lessons.saveAndFlush(aula);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removerAula(
            @PathVariable("lessonId") UUID lessonId) {
        lessons.delete(busca.aula(lessonId));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---------- materiais complementares ----------

    @PostMapping("/lessons/{lessonId}/materials")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> criarMaterial(
            @PathVariable("lessonId") UUID lessonId, @Valid @RequestBody MaterialRequest req) {
        Lesson aula = busca.aula(lessonId);
        materials.saveAndFlush(new LessonMaterial(aula.getId(), req.title().trim(),
                req.url().trim(), req.position()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @DeleteMapping("/materials/{materialId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removerMaterial(
            @PathVariable("materialId") UUID materialId) {
        LessonMaterial material = materials.findById(materialId)
                .orElseThrow(() -> new NotFoundException("Material não encontrado"));
        materials.delete(material);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---------- auxiliares ----------

    private CourseAdminResponse responder(Course curso) {
        List<UUID> produtoIds = courseProducts.findByCourseId(curso.getId()).stream()
                .map(CourseProduct::getProductId)
                .toList();
        List<UUID> moduloIds = modules.findByCourseIdOrderByPositionAscTitleAsc(curso.getId())
                .stream().map(CourseModule::getId).toList();
        long aulas = moduloIds.isEmpty() ? 0 : lessons.countByModuleIdIn(moduloIds);

        return new CourseAdminResponse(curso.getId(), curso.getTitle(), curso.getSlug(),
                curso.getDescription(), curso.getCoverUrl(), curso.isPublished(),
                produtoIds, moduloIds.size(), aulas);
    }

    /** "Mentoria BB 2026" -> "mentoria-bb-2026". */
    private static String slugDe(String titulo) {
        String semAcentos = Normalizer.normalize(titulo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
