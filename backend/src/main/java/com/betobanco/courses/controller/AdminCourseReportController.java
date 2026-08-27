package com.betobanco.courses.controller;

import com.betobanco.courses.dto.CourseLessonReportResponse;
import com.betobanco.courses.dto.CourseReportResponse;
import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseModule;
import com.betobanco.courses.entity.CourseProduct;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonComment;
import com.betobanco.courses.entity.LessonProgress;
import com.betobanco.courses.entity.LessonRating;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.CourseProductRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.repository.LessonCommentRepository;
import com.betobanco.courses.repository.LessonProgressRepository;
import com.betobanco.courses.repository.LessonRatingRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.courses.service.AdminCourseLookup;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Relatorios de engajamento do professor. Somente leitura, agregado em
 * memoria: a escala atual (centenas de registros) nao justifica SQL
 * analitico — quando justificar, este controller e o unico lugar a mudar.
 */
@RestController
@RequestMapping("/admin/courses")
@Tag(name = "Admin - Reports")
public class AdminCourseReportController {

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final CourseProductRepository courseProducts;
    private final LessonProgressRepository progress;
    private final LessonRatingRepository ratings;
    private final LessonCommentRepository comments;
    private final EntitlementService entitlements;
    private final AdminCourseLookup busca;

    public AdminCourseReportController(CourseRepository courses,
                                       CourseModuleRepository modules,
                                       LessonRepository lessons,
                                       CourseProductRepository courseProducts,
                                       LessonProgressRepository progress,
                                       LessonRatingRepository ratings,
                                       LessonCommentRepository comments,
                                       EntitlementService entitlements,
                                       AdminCourseLookup busca) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.courseProducts = courseProducts;
        this.progress = progress;
        this.ratings = ratings;
        this.comments = comments;
        this.entitlements = entitlements;
        this.busca = busca;
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CourseReportResponse>>> geral() {
        List<CourseReportResponse> linhas = courses.findAll(Sort.by("title")).stream()
                .map(this::linhaDoCurso)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(linhas));
    }

    @GetMapping("/{id}/reports")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<CourseLessonReportResponse>> porAula(
            @PathVariable("id") UUID id) {
        Course curso = busca.curso(id);

        List<UUID> alunos = alunosDoCurso(curso.getId());
        List<CourseModule> modulosDoCurso =
                modules.findByCourseIdOrderByPositionAscTitleAsc(curso.getId());
        Map<UUID, String> tituloModulo = modulosDoCurso.stream()
                .collect(Collectors.toMap(CourseModule::getId, CourseModule::getTitle));
        List<Lesson> aulas = modulosDoCurso.isEmpty() ? List.of()
                : lessons.findByModuleIdInOrderByPositionAscTitleAsc(
                        modulosDoCurso.stream().map(CourseModule::getId).toList());
        List<UUID> aulaIds = aulas.stream().map(Lesson::getId).toList();

        // Uma busca em lote por metrica; agrupamento em memoria.
        Map<UUID, Long> conclusoes = aulaIds.isEmpty() ? Map.of()
                : progress.findByLessonIdIn(aulaIds).stream()
                        .collect(Collectors.groupingBy(LessonProgress::getLessonId,
                                Collectors.counting()));
        Map<UUID, List<LessonRating>> votos = aulaIds.isEmpty() ? Map.of()
                : ratings.findByLessonIdIn(aulaIds).stream()
                        .collect(Collectors.groupingBy(LessonRating::getLessonId));
        Map<UUID, Long> qtdComentarios = aulaIds.isEmpty() ? Map.of()
                : comments.findByLessonIdIn(aulaIds).stream()
                        .collect(Collectors.groupingBy(LessonComment::getLessonId,
                                Collectors.counting()));

        long totalAlunos = alunos.size();
        List<CourseLessonReportResponse.LessonLine> linhas = aulas.stream()
                .map(a -> {
                    long feitas = conclusoes.getOrDefault(a.getId(), 0L);
                    List<LessonRating> vs = votos.getOrDefault(a.getId(), List.of());
                    return new CourseLessonReportResponse.LessonLine(a.getId(), a.getTitle(),
                            tituloModulo.get(a.getModuleId()), feitas,
                            totalAlunos == 0 ? 0 : (int) Math.round(feitas * 100.0 / totalAlunos),
                            vs.stream().filter(LessonRating::isHelpful).count(),
                            vs.stream().filter(v -> !v.isHelpful()).count(),
                            qtdComentarios.getOrDefault(a.getId(), 0L));
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(new CourseLessonReportResponse(
                curso.getId(), curso.getTitle(), totalAlunos, linhas)));
    }

    private List<UUID> alunosDoCurso(UUID courseId) {
        List<UUID> produtoIds = courseProducts.findByCourseId(courseId).stream()
                .map(CourseProduct::getProductId)
                .toList();
        return entitlements.usuariosComAcesso(produtoIds);
    }

    private CourseReportResponse linhaDoCurso(Course curso) {
        List<UUID> alunos = alunosDoCurso(curso.getId());
        List<CourseModule> modulosDoCurso =
                modules.findByCourseIdOrderByPositionAscTitleAsc(curso.getId());
        List<Lesson> aulas = modulosDoCurso.isEmpty() ? List.of()
                : lessons.findByModuleIdInOrderByPositionAscTitleAsc(
                        modulosDoCurso.stream().map(CourseModule::getId).toList());
        List<UUID> aulaIds = aulas.stream().map(Lesson::getId).toList();

        List<LessonProgress> feitas = aulaIds.isEmpty() ? List.of()
                : progress.findByLessonIdIn(aulaIds);
        Set<UUID> iniciaram = feitas.stream()
                .map(LessonProgress::getUserId)
                .collect(Collectors.toSet());

        long possiveis = (long) alunos.size() * aulaIds.size();
        int mediaPct = possiveis == 0 ? 0 : (int) Math.round(feitas.size() * 100.0 / possiveis);

        return new CourseReportResponse(curso.getId(), curso.getTitle(), curso.isPublished(),
                alunos.size(), iniciaram.size(), aulaIds.size(), mediaPct);
    }
}
