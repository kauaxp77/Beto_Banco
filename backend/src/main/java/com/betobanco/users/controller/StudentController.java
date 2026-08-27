package com.betobanco.users.controller;

import com.betobanco.catalog.api.ProductCatalog;
import com.betobanco.entitlements.api.EntitlementService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.dto.EntitlementResponse;
import com.betobanco.users.dto.StudentResponse;
import com.betobanco.users.dto.StudentUpdateRequest;
import com.betobanco.users.entity.Student;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Este controller vive dentro do modulo {@code users}, entao pode usar as
 * entidades e os repositorios dele — a regra ArchUnit proibe atravessar a
 * fronteira de OUTRO modulo, nao a do proprio.
 *
 * <p>Nenhum metodo daqui, nem privado, devolve {@code @Entity}: a regra
 * {@code nenhumControllerRetornaEntidadeJpa} inspeciona todos os metodos da
 * classe, e um auxiliar que devolvesse {@code User} a reprovaria.
 */
@RestController
@RequestMapping("/students")
@Tag(name = "Students")
public class StudentController {

    private final UserRepository users;
    private final StudentRepository students;
    private final EntitlementService entitlements;
    private final ProductCatalog catalogo;

    public StudentController(UserRepository users, StudentRepository students,
                             EntitlementService entitlements, ProductCatalog catalogo) {
        this.users = users;
        this.students = students;
        this.entitlements = entitlements;
        this.catalogo = catalogo;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<StudentResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        // Sempre pelo id do token. Nao existe rota que aceite id de aluno.
        User usuario = users.findById(atual.id())
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));
        String telefone = students.findById(atual.id()).map(Student::getPhone).orElse(null);

        return ResponseEntity.ok(ApiResponse.ok(new StudentResponse(
                usuario.getId(), usuario.getEmail(), usuario.getFullName(), telefone)));
    }

    @GetMapping("/me/entitlements")
    public ResponseEntity<ApiResponse<List<EntitlementResponse>>> meusEntitlements(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        // Sempre pelo id do token; um produto apagado do catalogo nao derruba
        // a listagem, so aparece sem nome.
        List<EntitlementResponse> itens = entitlements.listarDe(atual.id()).stream()
                .map(e -> {
                    var produto = catalogo.buscarPorId(e.productId()).orElse(null);
                    return new EntitlementResponse(e.entitlementId(), e.productId(),
                            produto == null ? null : produto.sku(),
                            produto == null ? null : produto.name(),
                            e.source(), e.grantedAt(), e.expiresAt());
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(itens));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<ApiResponse<StudentResponse>> atualizar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody StudentUpdateRequest req) {

        User usuario = users.findById(atual.id())
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));
        usuario.setFullName(req.fullName().trim());
        users.saveAndFlush(usuario);

        Student perfil = students.findById(atual.id()).orElseGet(() -> new Student(atual.id()));
        perfil.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone());
        students.saveAndFlush(perfil);

        return ResponseEntity.ok(ApiResponse.ok(new StudentResponse(
                usuario.getId(), usuario.getEmail(), usuario.getFullName(), perfil.getPhone())));
    }
}
