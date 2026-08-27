package com.betobanco.courses.controller;

import com.betobanco.courses.dto.CertificateResponse;
import com.betobanco.courses.dto.TestimonialResponse;
import com.betobanco.courses.service.StudentEngagementService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints sem login (liberados no SecurityConfig): a validacao do
 * certificado existe para terceiros — um recrutador com o codigo — e os
 * depoimentos aprovados alimentam a prova social da landing page.
 */
@RestController
@Tag(name = "Public - Engagement")
public class PublicEngagementController {

    private final StudentEngagementService engajamento;

    public PublicEngagementController(StudentEngagementService engajamento) {
        this.engajamento = engajamento;
    }

    @GetMapping("/certificates/{code}")
    public ResponseEntity<ApiResponse<CertificateResponse>> validar(
            @PathVariable("code") String code) {
        return ResponseEntity.ok(ApiResponse.ok(engajamento.validar(code)));
    }

    @GetMapping("/testimonials")
    public ResponseEntity<ApiResponse<List<TestimonialResponse>>> aprovados() {
        return ResponseEntity.ok(ApiResponse.ok(engajamento.aprovados()));
    }
}
