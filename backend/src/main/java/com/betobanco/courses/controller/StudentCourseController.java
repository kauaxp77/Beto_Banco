package com.betobanco.courses.controller;

import com.betobanco.courses.dto.AnnouncementResponse;
import com.betobanco.courses.dto.CertificateResponse;
import com.betobanco.courses.dto.CommentCreateRequest;
import com.betobanco.courses.dto.CourseDetailResponse;
import com.betobanco.courses.dto.CourseSummaryResponse;
import com.betobanco.courses.dto.LessonDiscussionResponse;
import com.betobanco.courses.dto.RatingRequest;
import com.betobanco.courses.dto.StudyStatsResponse;
import com.betobanco.courses.dto.TestimonialCreateRequest;
import com.betobanco.courses.dto.TestimonialResponse;
import com.betobanco.courses.dto.TrackResponse;
import com.betobanco.courses.service.StudentCourseService;
import com.betobanco.courses.service.StudentEngagementService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A area de membros do aluno. A identidade vem sempre do token; o servico
 * decide o que este usuario pode ver a partir dos entitlements dele.
 */
@RestController
@RequestMapping("/courses")
@Tag(name = "Courses")
public class StudentCourseController {

    private final StudentCourseService cursos;
    private final StudentEngagementService engajamento;

    public StudentCourseController(StudentCourseService cursos,
                                   StudentEngagementService engajamento) {
        this.cursos = cursos;
        this.engajamento = engajamento;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CourseSummaryResponse>>> meusCursos(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(cursos.listarMeusCursos(atual.id())));
    }

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> anuncios(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(cursos.listarAnuncios(atual.id())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> detalhar(
            @AuthenticationPrincipal AuthenticatedUser atual, @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(cursos.detalhar(atual.id(), id)));
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Void>> concluir(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId) {
        cursos.concluirAula(atual.id(), lessonId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Void>> desfazer(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId) {
        cursos.desfazerConclusao(atual.id(), lessonId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---------- certificados, trilhas e constancia (Fase 4) ----------

    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<StudyStatsResponse>> estatisticas(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(engajamento.estatisticas(atual.id())));
    }

    @GetMapping("/me/tracks")
    public ResponseEntity<ApiResponse<List<TrackResponse>>> minhasTrilhas(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(engajamento.minhasTrilhas(atual.id())));
    }

    @GetMapping("/me/certificates")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> meusCertificados(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(engajamento.meusCertificados(atual.id())));
    }

    @PostMapping("/{id}/certificate")
    public ResponseEntity<ApiResponse<CertificateResponse>> emitirCertificado(
            @AuthenticationPrincipal AuthenticatedUser atual, @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                engajamento.emitirCertificado(atual.id(), id)));
    }

    @GetMapping("/me/testimonials")
    public ResponseEntity<ApiResponse<List<TestimonialResponse>>> meusDepoimentos(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(engajamento.meusDepoimentos(atual.id())));
    }

    @PostMapping("/testimonials")
    public ResponseEntity<ApiResponse<TestimonialResponse>> enviarDepoimento(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody TestimonialCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(engajamento.enviarDepoimento(atual.id(), req)));
    }

    // ---------- discussao da aula (Fase 2) ----------

    @GetMapping("/lessons/{lessonId}/discussion")
    public ResponseEntity<ApiResponse<LessonDiscussionResponse>> discussao(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.ok(cursos.discussao(atual.id(), lessonId)));
    }

    @PostMapping("/lessons/{lessonId}/comments")
    public ResponseEntity<ApiResponse<Map<String, UUID>>> comentar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody CommentCreateRequest req) {
        UUID id = cursos.comentar(atual.id(), lessonId, req.body(), req.parentId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("id", id)));
    }

    @PutMapping("/lessons/{lessonId}/rating")
    public ResponseEntity<ApiResponse<Void>> avaliar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody RatingRequest req) {
        cursos.avaliar(atual.id(), lessonId, req.helpful());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
