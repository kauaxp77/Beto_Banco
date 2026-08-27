package com.betobanco.courses.controller;

import com.betobanco.courses.dto.AnnouncementCreateRequest;
import com.betobanco.courses.dto.AnnouncementResponse;
import com.betobanco.courses.entity.Announcement;
import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseProduct;
import com.betobanco.courses.repository.AnnouncementRepository;
import com.betobanco.courses.repository.CourseProductRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.service.AdminCourseLookup;
import com.betobanco.email.api.EmailService;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.api.UserDirectory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comunicacao do professor com a turma. O e-mail em massa reusa a outbox: o
 * anuncio e gravado na mesma transacao, e as mensagens saem depois pelo
 * worker — um SMTP fora do ar nao desfaz o anuncio.
 */
@RestController
@RequestMapping("/admin/courses/announcements")
@Tag(name = "Admin - Announcements")
public class AdminAnnouncementController {

    private final AnnouncementRepository announcements;
    private final CourseRepository courses;
    private final CourseProductRepository courseProducts;
    private final AdminCourseLookup busca;
    private final EntitlementService entitlements;
    private final UserDirectory usuarios;
    private final EmailService emails;

    public AdminAnnouncementController(AnnouncementRepository announcements,
                                       CourseRepository courses,
                                       CourseProductRepository courseProducts,
                                       AdminCourseLookup busca,
                                       EntitlementService entitlements,
                                       UserDirectory usuarios, EmailService emails) {
        this.announcements = announcements;
        this.courses = courses;
        this.courseProducts = courseProducts;
        this.busca = busca;
        this.entitlements = entitlements;
        this.usuarios = usuarios;
        this.emails = emails;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> listar() {
        List<AnnouncementResponse> todos = announcements.findAllByOrderByCreatedAtDesc().stream()
                .map(this::responder)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(todos));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<AnnouncementResponse>> criar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody AnnouncementCreateRequest req) {

        if (req.courseId() != null) {
            busca.curso(req.courseId());
        }

        Announcement criado = announcements.saveAndFlush(new Announcement(
                req.courseId(), req.title().trim(), req.body().trim(), atual.id()));

        // E-mail em massa so para anuncio de curso: a lista de destinatarios
        // vem dos entitlements dos produtos que vendem o curso.
        if (Boolean.TRUE.equals(req.sendEmail()) && req.courseId() != null) {
            List<UUID> produtoIds = courseProducts.findByCourseId(req.courseId()).stream()
                    .map(CourseProduct::getProductId)
                    .toList();
            for (UUID userId : entitlements.usuariosComAcesso(produtoIds)) {
                usuarios.buscarAtivoPorId(userId).ifPresent(aluno ->
                        emails.enfileirar(aluno.email(), EmailService.Templates.ANUNCIO,
                                Map.of("nome", aluno.fullName(),
                                        "titulo", criado.getTitle(),
                                        "mensagem", criado.getBody()),
                                "anuncio:" + criado.getId() + ":" + userId));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(responder(criado)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable("id") UUID id) {
        Announcement anuncio = announcements.findById(id)
                .orElseThrow(() -> new NotFoundException("Anúncio não encontrado"));
        announcements.delete(anuncio);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private AnnouncementResponse responder(Announcement a) {
        String tituloCurso = a.getCourseId() == null ? null
                : courses.findById(a.getCourseId()).map(Course::getTitle).orElse(null);
        return new AnnouncementResponse(a.getId(), a.getCourseId(), tituloCurso,
                a.getTitle(), a.getBody(), a.getCreatedAt());
    }
}
