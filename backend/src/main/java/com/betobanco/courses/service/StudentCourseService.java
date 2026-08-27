package com.betobanco.courses.service;

import com.betobanco.courses.dto.CourseDetailResponse;
import com.betobanco.courses.dto.CourseSummaryResponse;
import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseModule;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonProgress;
import com.betobanco.courses.dto.AnnouncementResponse;
import com.betobanco.courses.dto.LessonDiscussionResponse;
import com.betobanco.courses.entity.Announcement;
import com.betobanco.courses.entity.LessonComment;
import com.betobanco.courses.entity.LessonMaterial;
import com.betobanco.courses.entity.LessonRating;
import com.betobanco.courses.repository.AnnouncementRepository;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.repository.LessonCommentRepository;
import com.betobanco.courses.repository.LessonMaterialRepository;
import com.betobanco.courses.repository.LessonProgressRepository;
import com.betobanco.courses.repository.LessonRatingRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tudo que o aluno ve passa por aqui, e toda resposta comeca pela mesma
 * pergunta: quais produtos este usuario comprou? Curso sem vinculo com um
 * entitlement vigente simplesmente nao existe para ele — inclusive o 404
 * de curso alheio e identico ao de curso inexistente, para nao vazar
 * catalogo.
 */
@Service
public class StudentCourseService {

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final LessonCommentRepository comments;
    private final LessonMaterialRepository materials;
    private final LessonRatingRepository ratings;
    private final AnnouncementRepository announcements;
    private final UserDirectory usuarios;
    private final CourseAccess acesso;

    public StudentCourseService(CourseRepository courses, CourseModuleRepository modules,
                                LessonRepository lessons,
                                LessonProgressRepository progress,
                                LessonCommentRepository comments,
                                LessonMaterialRepository materials,
                                LessonRatingRepository ratings,
                                AnnouncementRepository announcements,
                                UserDirectory usuarios, CourseAccess acesso) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.progress = progress;
        this.comments = comments;
        this.materials = materials;
        this.ratings = ratings;
        this.announcements = announcements;
        this.usuarios = usuarios;
        this.acesso = acesso;
    }

    private Set<UUID> cursosAcessiveis(UUID userId) {
        return acesso.cursosAcessiveis(userId);
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryResponse> listarMeusCursos(UUID userId) {
        Set<UUID> acessiveis = cursosAcessiveis(userId);
        if (acessiveis.isEmpty()) {
            return List.of();
        }

        List<Course> meusCursos = courses.findByIdInAndPublishedTrueOrderByTitle(acessiveis);
        List<UUID> courseIds = meusCursos.stream().map(Course::getId).toList();

        // Duas buscas em lote no lugar de N por curso.
        List<CourseModule> todosModulos =
                modules.findByCourseIdInOrderByPositionAscTitleAsc(courseIds);
        Map<UUID, List<CourseModule>> modulosPorCurso = todosModulos.stream()
                .collect(Collectors.groupingBy(CourseModule::getCourseId));

        List<Lesson> todasAulas = todosModulos.isEmpty() ? List.of()
                : lessons.findByModuleIdInOrderByPositionAscTitleAsc(
                        todosModulos.stream().map(CourseModule::getId).toList());
        Map<UUID, List<Lesson>> aulasPorModulo = todasAulas.stream()
                .filter(Lesson::isPublished)
                .collect(Collectors.groupingBy(Lesson::getModuleId));

        Set<UUID> concluidas = todasAulas.isEmpty() ? Set.of()
                : progress.findByUserIdAndLessonIdIn(userId,
                                todasAulas.stream().map(Lesson::getId).toList()).stream()
                        .map(LessonProgress::getLessonId)
                        .collect(Collectors.toSet());

        return meusCursos.stream().map(curso -> {
            List<Lesson> aulasDoCurso = modulosPorCurso
                    .getOrDefault(curso.getId(), List.of()).stream()
                    .flatMap(m -> aulasPorModulo.getOrDefault(m.getId(), List.of()).stream())
                    .toList();

            long total = aulasDoCurso.size();
            long feitas = aulasDoCurso.stream().filter(a -> concluidas.contains(a.getId())).count();
            UUID proxima = aulasDoCurso.stream()
                    .filter(a -> !concluidas.contains(a.getId()))
                    .map(Lesson::getId)
                    .findFirst()
                    .orElse(null);

            return new CourseSummaryResponse(curso.getId(), curso.getTitle(), curso.getSlug(),
                    curso.getDescription(), curso.getCoverUrl(), total, feitas, proxima);
        }).toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse detalhar(UUID userId, UUID courseId) {
        Course curso = exigirAcesso(userId, courseId);

        List<CourseModule> modulosDoCurso =
                modules.findByCourseIdOrderByPositionAscTitleAsc(courseId);
        List<Lesson> todasAulas = modulosDoCurso.isEmpty() ? List.of()
                : lessons.findByModuleIdInOrderByPositionAscTitleAsc(
                        modulosDoCurso.stream().map(CourseModule::getId).toList());
        Map<UUID, List<Lesson>> aulasPorModulo = todasAulas.stream()
                .filter(Lesson::isPublished)
                .collect(Collectors.groupingBy(Lesson::getModuleId));

        Set<UUID> concluidas = todasAulas.isEmpty() ? Set.of()
                : progress.findByUserIdAndLessonIdIn(userId,
                                todasAulas.stream().map(Lesson::getId).toList()).stream()
                        .map(LessonProgress::getLessonId)
                        .collect(Collectors.toSet());

        Map<UUID, List<LessonMaterial>> materiaisPorAula = todasAulas.isEmpty() ? Map.of()
                : materials.findByLessonIdInOrderByPositionAscTitleAsc(
                                todasAulas.stream().map(Lesson::getId).toList()).stream()
                        .collect(Collectors.groupingBy(LessonMaterial::getLessonId));

        List<CourseDetailResponse.ModuleResponse> modulosResposta = modulosDoCurso.stream()
                .map(m -> new CourseDetailResponse.ModuleResponse(m.getId(), m.getTitle(),
                        m.getPosition(),
                        aulasPorModulo.getOrDefault(m.getId(), List.of()).stream()
                                .map(a -> new CourseDetailResponse.LessonResponse(a.getId(),
                                        a.getTitle(), a.getDescription(), a.getVideoUrl(),
                                        a.getDurationSeconds(), a.getPosition(),
                                        concluidas.contains(a.getId()),
                                        materiaisPorAula.getOrDefault(a.getId(), List.of())
                                                .stream()
                                                .map(mat -> new CourseDetailResponse
                                                        .MaterialResponse(mat.getId(),
                                                                mat.getTitle(), mat.getUrl()))
                                                .toList()))
                                .toList()))
                // Modulo sem aula publicada nao aparece para o aluno.
                .filter(m -> !m.lessons().isEmpty())
                .toList();

        return new CourseDetailResponse(curso.getId(), curso.getTitle(), curso.getDescription(),
                curso.getCoverUrl(), modulosResposta);
    }

    /** Idempotente: concluir a mesma aula duas vezes nao duplica nem falha. */
    @Transactional
    public void concluirAula(UUID userId, UUID lessonId) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);
        if (progress.findByUserIdAndLessonId(userId, aula.getId()).isPresent()) {
            return;
        }
        try {
            progress.saveAndFlush(new LessonProgress(userId, aula.getId()));
        } catch (DataIntegrityViolationException e) {
            // Corrida com outra aba do mesmo aluno: a unique ja garantiu.
        }
    }

    @Transactional
    public void desfazerConclusao(UUID userId, UUID lessonId) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);
        progress.findByUserIdAndLessonId(userId, aula.getId()).ifPresent(progress::delete);
    }

    // ---------- anuncios (Fase 3) ----------

    /** Gerais + dos cursos que o aluno acessa, mais recentes primeiro (max 20). */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listarAnuncios(UUID userId) {
        Set<UUID> acessiveis = cursosAcessiveis(userId);
        List<Announcement> lista = acessiveis.isEmpty()
                ? announcements.gerais()
                : announcements.paraCursos(acessiveis);

        Map<UUID, String> titulos = acessiveis.isEmpty() ? Map.of()
                : courses.findAllById(acessiveis).stream()
                        .collect(Collectors.toMap(Course::getId, Course::getTitle));

        return lista.stream()
                .limit(20)
                .map(a -> new AnnouncementResponse(a.getId(), a.getCourseId(),
                        a.getCourseId() == null ? null : titulos.get(a.getCourseId()),
                        a.getTitle(), a.getBody(), a.getCreatedAt()))
                .toList();
    }

    // ---------- discussao da aula (Fase 2) ----------

    @Transactional(readOnly = true)
    public LessonDiscussionResponse discussao(UUID userId, UUID lessonId) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);

        List<LessonComment> visiveis = comments
                .findByLessonIdAndStatusOrderByCreatedAtAsc(aula.getId(), LessonComment.VISIBLE);

        // Cache local de autores: um lookup por autor distinto, nao por comentario.
        Map<UUID, Optional<UserAccount>> autores = new HashMap<>();
        List<LessonDiscussionResponse.CommentResponse> lista = visiveis.stream()
                .map(c -> {
                    Optional<UserAccount> autor = autores.computeIfAbsent(c.getUserId(),
                            usuarios::buscarAtivoPorId);
                    boolean instrutor = autor.map(a -> a.roles().contains("ROLE_ADMIN")
                            || a.roles().contains("ROLE_INSTRUCTOR")).orElse(false);
                    return new LessonDiscussionResponse.CommentResponse(c.getId(),
                            c.getParentId(), c.getBody(),
                            autor.map(UserAccount::fullName).orElse("Aluno"),
                            instrutor, c.getUserId().equals(userId), c.getCreatedAt());
                })
                .toList();

        Boolean meuVoto = ratings.findByUserIdAndLessonId(userId, aula.getId())
                .map(LessonRating::isHelpful)
                .orElse(null);

        return new LessonDiscussionResponse(lista,
                ratings.countByLessonIdAndHelpful(aula.getId(), true),
                ratings.countByLessonIdAndHelpful(aula.getId(), false),
                meuVoto);
    }

    @Transactional
    public UUID comentar(UUID userId, UUID lessonId, String body, UUID parentId) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);

        if (parentId != null) {
            // Resposta so pode apontar para comentario da MESMA aula.
            comments.findById(parentId)
                    .filter(pai -> pai.getLessonId().equals(aula.getId()))
                    .orElseThrow(() -> new NotFoundException("Comentário não encontrado"));
        }
        LessonComment novo = comments.saveAndFlush(
                new LessonComment(aula.getId(), userId, parentId, body.trim()));
        return novo.getId();
    }

    /** Upsert: votar de novo troca o voto, nunca duplica. */
    @Transactional
    public void avaliar(UUID userId, UUID lessonId, boolean helpful) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);
        LessonRating voto = ratings.findByUserIdAndLessonId(userId, aula.getId())
                .orElseGet(() -> new LessonRating(aula.getId(), userId, helpful));
        voto.setHelpful(helpful);
        try {
            ratings.saveAndFlush(voto);
        } catch (DataIntegrityViolationException e) {
            // Corrida entre abas do mesmo aluno: a unique ja resolveu.
        }
    }

    private Course exigirAcesso(UUID userId, UUID courseId) {
        Course curso = courses.findById(courseId)
                .filter(Course::isPublished)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado"));
        if (!cursosAcessiveis(userId).contains(curso.getId())) {
            throw new NotFoundException("Curso não encontrado");
        }
        return curso;
    }

    private Lesson exigirAulaAcessivel(UUID userId, UUID lessonId) {
        Lesson aula = lessons.findById(lessonId)
                .filter(Lesson::isPublished)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada"));
        CourseModule modulo = modules.findById(aula.getModuleId())
                .orElseThrow(() -> new NotFoundException("Aula não encontrada"));
        if (!cursosAcessiveis(userId).contains(modulo.getCourseId())) {
            throw new NotFoundException("Aula não encontrada");
        }
        return aula;
    }
}
