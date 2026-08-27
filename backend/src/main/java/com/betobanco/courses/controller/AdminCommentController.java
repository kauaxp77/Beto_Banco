package com.betobanco.courses.controller;

import com.betobanco.courses.dto.CommentAdminResponse;
import com.betobanco.courses.dto.CommentCreateRequest;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonComment;
import com.betobanco.courses.repository.LessonCommentRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.courses.service.AdminCourseLookup;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.pagination.PageRequestFactory;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.response.PageResponse;
import com.betobanco.users.api.UserAccount;
import com.betobanco.users.api.UserDirectory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Fila de moderacao do professor. Ocultar e reversivel e nada e apagado:
 * comentario e conversa com aluno, e conversa some da tela, nao do historico.
 */
@RestController
@RequestMapping("/admin/courses/comments")
@Tag(name = "Admin - Comments")
public class AdminCommentController {

    private final LessonCommentRepository comments;
    private final LessonRepository lessons;
    private final UserDirectory usuarios;
    private final AdminCourseLookup busca;

    public AdminCommentController(LessonCommentRepository comments, LessonRepository lessons,
                                  UserDirectory usuarios, AdminCourseLookup busca) {
        this.comments = comments;
        this.lessons = lessons;
        this.usuarios = usuarios;
        this.busca = busca;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<CommentAdminResponse>> listar(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        Pageable paginacao = PageRequestFactory.of(page, size, null);
        Page<LessonComment> pagina = (status == null || status.isBlank())
                ? comments.findAllByOrderByCreatedAtDesc(paginacao)
                : comments.findByStatusOrderByCreatedAtDesc(status.trim(), paginacao);

        return ResponseEntity.ok(PageResponse.from(pagina.map(this::responder)));
    }

    @PostMapping("/{id}/hide")
    @Transactional
    public ResponseEntity<ApiResponse<CommentAdminResponse>> ocultar(
            @PathVariable("id") UUID id) {
        return mudarStatus(id, LessonComment.HIDDEN);
    }

    @PostMapping("/{id}/show")
    @Transactional
    public ResponseEntity<ApiResponse<CommentAdminResponse>> reexibir(
            @PathVariable("id") UUID id) {
        return mudarStatus(id, LessonComment.VISIBLE);
    }

    /** Resposta oficial: entra como comentario do proprio admin logado. */
    @PostMapping("/{id}/reply")
    @Transactional
    public ResponseEntity<ApiResponse<CommentAdminResponse>> responderComentario(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("id") UUID id,
            @Valid @RequestBody CommentCreateRequest req) {
        LessonComment alvo = busca.comentario(id);
        LessonComment resposta = comments.saveAndFlush(new LessonComment(
                alvo.getLessonId(), atual.id(), alvo.getId(), req.body().trim()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(responder(resposta)));
    }

    private ResponseEntity<ApiResponse<CommentAdminResponse>> mudarStatus(UUID id,
                                                                          String status) {
        LessonComment comentario = busca.comentario(id);
        comentario.setStatus(status);
        comments.saveAndFlush(comentario);
        return ResponseEntity.ok(ApiResponse.ok(responder(comentario)));
    }

    private CommentAdminResponse responder(LessonComment c) {
        String tituloAula = lessons.findById(c.getLessonId()).map(Lesson::getTitle).orElse("—");
        Optional<UserAccount> autor = usuarios.buscarAtivoPorId(c.getUserId());
        return new CommentAdminResponse(c.getId(), c.getLessonId(), tituloAula, c.getParentId(),
                c.getBody(),
                autor.map(UserAccount::fullName).orElse("Aluno removido"),
                autor.map(UserAccount::email).orElse(null),
                c.getStatus(), c.getCreatedAt());
    }
}
