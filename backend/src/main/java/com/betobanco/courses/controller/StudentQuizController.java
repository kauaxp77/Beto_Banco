package com.betobanco.courses.controller;

import com.betobanco.courses.dto.QuizDtos;
import com.betobanco.courses.service.QuizService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Questoes da aula para o aluno: enunciados antes, gabarito so na entrega. */
@RestController
@RequestMapping("/courses/lessons/{lessonId}/quiz")
@Tag(name = "Courses - Quiz")
public class StudentQuizController {

    private final QuizService quiz;

    public StudentQuizController(QuizService quiz) {
        this.quiz = quiz;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<QuizDtos.QuizResponse>> ver(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.ok(quiz.doAluno(atual.id(), lessonId)));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<QuizDtos.ResultResponse>> entregar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody QuizDtos.SubmitRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(quiz.entregar(atual.id(), lessonId, req)));
    }
}
