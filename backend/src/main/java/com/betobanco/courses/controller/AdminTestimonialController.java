package com.betobanco.courses.controller;

import com.betobanco.courses.dto.TestimonialResponse;
import com.betobanco.courses.entity.Testimonial;
import com.betobanco.courses.repository.TestimonialRepository;
import com.betobanco.courses.service.StudentEngagementService;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Moderacao da prova social. Aprovar publica na vitrine; ocultar tira do ar
 * sem apagar — o aluno escreveu, o registro fica.
 */
@RestController
@RequestMapping("/admin/courses/testimonials")
@Tag(name = "Admin - Testimonials")
public class AdminTestimonialController {

    private static final Set<String> STATUS_VALIDOS =
            Set.of(Testimonial.PENDING, Testimonial.APPROVED, Testimonial.HIDDEN);

    private final TestimonialRepository testimonials;
    private final StudentEngagementService engajamento;

    public AdminTestimonialController(TestimonialRepository testimonials,
                                      StudentEngagementService engajamento) {
        this.testimonials = testimonials;
        this.engajamento = engajamento;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<TestimonialResponse>>> listar(
            @RequestParam(value = "status", required = false) String status) {
        List<Testimonial> lista = (status == null || status.isBlank())
                ? testimonials.findAllByOrderByCreatedAtDesc()
                : testimonials.findByStatusOrderByCreatedAtDesc(status.trim());
        return ResponseEntity.ok(ApiResponse.ok(
                lista.stream().map(engajamento::responderDepoimento).toList()));
    }

    @PostMapping("/{id}/status/{novo}")
    @Transactional
    public ResponseEntity<ApiResponse<TestimonialResponse>> moderar(
            @PathVariable("id") UUID id, @PathVariable("novo") String novo) {
        String statusNovo = novo.trim().toUpperCase();
        if (!STATUS_VALIDOS.contains(statusNovo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Status inválido: use PENDING, APPROVED ou HIDDEN");
        }
        Testimonial depoimento = testimonials.findById(id)
                .orElseThrow(() -> new NotFoundException("Depoimento não encontrado"));
        depoimento.setStatus(statusNovo);
        testimonials.saveAndFlush(depoimento);
        return ResponseEntity.ok(ApiResponse.ok(engajamento.responderDepoimento(depoimento)));
    }
}
