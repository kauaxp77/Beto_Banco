package com.betobanco.users.controller;

import com.betobanco.audit.api.AuditLogger;
import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.pagination.PageRequestFactory;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.response.PageResponse;
import com.betobanco.users.dto.GrantEntitlementRequest;
import com.betobanco.users.dto.StatusUpdateRequest;
import com.betobanco.users.dto.StudentDetailResponse;
import com.betobanco.users.dto.StudentSummaryResponse;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.Student;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Gestao de alunos pelo administrador. Toda acao que muda estado leva o
 * admin responsavel para a auditoria — "quem fez" e parte do requisito,
 * nao um extra.
 */
@RestController
@RequestMapping("/admin/students")
@Tag(name = "Admin - Students")
public class AdminStudentController {

    private final UserRepository users;
    private final StudentRepository students;
    private final EntitlementService entitlements;
    private final ProductCatalog catalogo;
    private final AuditLogger auditoria;

    public AdminStudentController(UserRepository users, StudentRepository students,
                                  EntitlementService entitlements, ProductCatalog catalogo,
                                  AuditLogger auditoria) {
        this.users = users;
        this.students = students;
        this.entitlements = entitlements;
        this.catalogo = catalogo;
        this.auditoria = auditoria;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<StudentSummaryResponse>> listar(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        var pagina = users.buscarAlunos(
                search == null ? "" : search.trim(),
                status == null ? "" : status.trim(),
                PageRequestFactory.of(page, size, "createdAt,desc"));

        return ResponseEntity.ok(PageResponse.from(pagina.map(this::resumo)));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<StudentDetailResponse>> detalhe(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(detalheDe(exigir(id))));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<StudentDetailResponse>> mudarStatus(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable("id") UUID id,
            @Valid @RequestBody StatusUpdateRequest req) {

        User aluno = exigir(id);
        aluno.setStatus(req.status());
        users.saveAndFlush(aluno);

        auditoria.registrarComAtor(admin.id(),
                User.BLOCKED.equals(req.status())
                        ? AuditLogger.Acoes.STUDENT_BLOCKED
                        : AuditLogger.Acoes.STUDENT_UNBLOCKED,
                "User", id.toString(), Map.of("status", req.status()));

        return ResponseEntity.ok(ApiResponse.ok(detalheDe(aluno)));
    }

    @PostMapping("/{id}/entitlements")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> conceder(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable("id") UUID id,
            @Valid @RequestBody GrantEntitlementRequest req) {

        User aluno = exigir(id);
        var produto = catalogo.buscarPorId(req.productId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        var concessao = entitlements.conceder(aluno.getId(), produto.id(),
                "MANUAL", "admin:" + admin.id());

        auditoria.registrarComAtor(admin.id(), AuditLogger.Acoes.ACCESS_GRANTED,
                "Entitlement", concessao.entitlementId().toString(),
                Map.of("productId", produto.id().toString(),
                        "criadoAgora", concessao.criadoAgora()));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                Map.of("entitlementId", concessao.entitlementId(),
                        "criadoAgora", concessao.criadoAgora())));
    }

    @DeleteMapping("/{id}/entitlements/{eid}")
    @Transactional
    public ResponseEntity<Void> revogar(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable("id") UUID id,
            @PathVariable("eid") UUID eid) {

        exigir(id);
        if (!entitlements.revogarPorId(id, eid)) {
            throw new NotFoundException("Entitlement não encontrado para este aluno");
        }

        auditoria.registrarComAtor(admin.id(), AuditLogger.Acoes.ACCESS_REVOKED,
                "Entitlement", eid.toString(), Map.of());

        return ResponseEntity.noContent().build();
    }

    private User exigir(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));
    }

    private StudentSummaryResponse resumo(User u) {
        return new StudentSummaryResponse(u.getId(), u.getEmail(), u.getFullName(),
                u.getStatus(), u.getCreatedAt());
    }

    private StudentDetailResponse detalheDe(User u) {
        String telefone = students.findById(u.getId()).map(Student::getPhone).orElse(null);
        return new StudentDetailResponse(u.getId(), u.getEmail(), u.getFullName(), telefone,
                u.getStatus(),
                u.getRoles().stream().map(Role::getName).sorted().toList(),
                u.getCreatedAt());
    }
}
