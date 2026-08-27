package com.betobanco.courses.controller;

import com.betobanco.courses.dto.QuizDtos;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.QuizQuestion;
import com.betobanco.courses.repository.QuizQuestionRepository;
import com.betobanco.courses.service.AdminCourseLookup;
import com.betobanco.courses.service.QuizService;
import com.betobanco.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Banco de questoes por aula — o QuizBuilder do professor. */
@RestController
@RequestMapping("/admin/courses")
@Tag(name = "Admin - Quiz")
public class AdminQuizController {

    private final QuizQuestionRepository questions;
    private final QuizService quiz;
    private final AdminCourseLookup busca;

    public AdminQuizController(QuizQuestionRepository questions, QuizService quiz,
                               AdminCourseLookup busca) {
        this.questions = questions;
        this.quiz = quiz;
        this.busca = busca;
    }

    @GetMapping("/lessons/{lessonId}/questions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<QuizDtos.QuestionAdmin>>> listar(
            @PathVariable("lessonId") UUID lessonId) {
        Lesson aula = busca.aula(lessonId);
        List<QuizDtos.QuestionAdmin> lista =
                questions.findByLessonIdOrderByPositionAsc(aula.getId()).stream()
                        .map(this::responder)
                        .toList();
        return ResponseEntity.ok(ApiResponse.ok(lista));
    }

    @PostMapping("/lessons/{lessonId}/questions")
    @Transactional
    public ResponseEntity<ApiResponse<QuizDtos.QuestionAdmin>> criar(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody QuizDtos.QuestionRequest req) {
        Lesson aula = busca.aula(lessonId);
        validarIndice(req);
        QuizQuestion criada = questions.saveAndFlush(new QuizQuestion(aula.getId(),
                req.statement().trim(), quiz.escreverOpcoes(req.options()),
                req.correctIndex(), req.explanation(), req.position()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(responder(criada)));
    }

    @PutMapping("/questions/{questionId}")
    @Transactional
    public ResponseEntity<ApiResponse<QuizDtos.QuestionAdmin>> atualizar(
            @PathVariable("questionId") UUID questionId,
            @Valid @RequestBody QuizDtos.QuestionRequest req) {
        QuizQuestion questao = busca.questao(questionId);
        validarIndice(req);
        questao.setStatement(req.statement().trim());
        questao.setOptions(quiz.escreverOpcoes(req.options()));
        questao.setCorrectIndex(req.correctIndex());
        questao.setExplanation(req.explanation());
        questao.setPosition(req.position());
        questions.saveAndFlush(questao);
        return ResponseEntity.ok(ApiResponse.ok(responder(questao)));
    }

    @DeleteMapping("/questions/{questionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> remover(
            @PathVariable("questionId") UUID questionId) {
        questions.delete(busca.questao(questionId));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void validarIndice(QuizDtos.QuestionRequest req) {
        if (req.correctIndex() >= req.options().size()) {
            throw new com.betobanco.shared.exception.BusinessException(
                    com.betobanco.shared.exception.ErrorCode.VALIDATION_ERROR,
                    "correctIndex aponta para uma alternativa que não existe");
        }
    }

    private QuizDtos.QuestionAdmin responder(QuizQuestion q) {
        return new QuizDtos.QuestionAdmin(q.getId(), q.getStatement(), quiz.lerOpcoes(q),
                q.getCorrectIndex(), q.getExplanation(), q.getPosition());
    }
}
