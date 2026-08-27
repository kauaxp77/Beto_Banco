package com.betobanco.courses.service;

import com.betobanco.courses.dto.QuizDtos;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonProgress;
import com.betobanco.courses.entity.QuizAttempt;
import com.betobanco.courses.entity.QuizQuestion;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.LessonProgressRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.courses.repository.QuizAttemptRepository;
import com.betobanco.courses.repository.QuizQuestionRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Motor de questoes das aulas. Regra de ouro: o gabarito NUNCA trafega
 * antes da entrega — a correcao acontece aqui, no servidor.
 */
@Service
public class QuizService {

    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final LessonRepository lessons;
    private final CourseModuleRepository modules;
    private final LessonProgressRepository progress;
    private final CourseAccess acesso;
    private final ObjectMapper json;

    public QuizService(QuizQuestionRepository questions, QuizAttemptRepository attempts,
                       LessonRepository lessons, CourseModuleRepository modules,
                       LessonProgressRepository progress, CourseAccess acesso,
                       ObjectMapper json) {
        this.questions = questions;
        this.attempts = attempts;
        this.lessons = lessons;
        this.modules = modules;
        this.progress = progress;
        this.acesso = acesso;
        this.json = json;
    }

    // ---------- aluno ----------

    @Transactional(readOnly = true)
    public QuizDtos.QuizResponse doAluno(UUID userId, UUID lessonId) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);

        List<QuizDtos.QuestionForStudent> lista =
                questions.findByLessonIdOrderByPositionAsc(aula.getId()).stream()
                        .map(q -> new QuizDtos.QuestionForStudent(q.getId(), q.getStatement(),
                                lerOpcoes(q), q.getPosition()))
                        .toList();

        List<QuizDtos.AttemptSummary> historico =
                attempts.findTop10ByUserIdAndLessonIdOrderByCreatedAtDesc(userId, aula.getId())
                        .stream()
                        .map(a -> new QuizDtos.AttemptSummary(a.getId(), a.getCorrectCount(),
                                a.getTotalCount(), a.getCreatedAt()))
                        .toList();

        return new QuizDtos.QuizResponse(lista, historico);
    }

    @Transactional
    public QuizDtos.ResultResponse entregar(UUID userId, UUID lessonId,
                                            QuizDtos.SubmitRequest req) {
        Lesson aula = exigirAulaAcessivel(userId, lessonId);
        List<QuizQuestion> todas = questions.findByLessonIdOrderByPositionAsc(aula.getId());
        if (todas.isEmpty()) {
            throw new NotFoundException("Esta aula não tem questões");
        }

        Map<UUID, Integer> respostas = new HashMap<>();
        for (QuizDtos.SubmitRequest.Answer r : req.answers()) {
            respostas.put(r.questionId(), r.answerIndex());
        }
        if (respostas.size() < todas.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Responda todas as questões antes de entregar");
        }

        int acertos = 0;
        List<QuizDtos.ResultItem> itens = new java.util.ArrayList<>(todas.size());
        for (QuizQuestion q : todas) {
            Integer minha = respostas.get(q.getId());
            if (minha == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Resposta ausente para uma das questões");
            }
            boolean correta = minha == q.getCorrectIndex();
            if (correta) {
                acertos++;
            }
            itens.add(new QuizDtos.ResultItem(q.getId(), minha, q.getCorrectIndex(), correta,
                    q.getExplanation()));
        }

        attempts.saveAndFlush(new QuizAttempt(userId, aula.getId(), acertos, todas.size(),
                escrever(respostas)));

        // Entregar o simulado conta como aula concluida — mesma idempotencia
        // do fluxo de video.
        if (progress.findByUserIdAndLessonId(userId, aula.getId()).isEmpty()) {
            try {
                progress.saveAndFlush(new LessonProgress(userId, aula.getId()));
            } catch (DataIntegrityViolationException e) {
                // corrida entre abas: a unique ja resolveu
            }
        }

        int pct = (int) Math.round(acertos * 100.0 / todas.size());
        return new QuizDtos.ResultResponse(acertos, todas.size(), pct, itens);
    }

    // ---------- leitura/escrita de JSON ----------

    public List<String> lerOpcoes(QuizQuestion q) {
        try {
            return json.readValue(q.getOptions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("opcoes ilegiveis na questao " + q.getId(), e);
        }
    }

    public String escreverOpcoes(List<String> opcoes) {
        try {
            return json.writeValueAsString(opcoes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("falha ao serializar opcoes", e);
        }
    }

    private String escrever(Map<UUID, Integer> respostas) {
        try {
            return json.writeValueAsString(respostas);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("falha ao serializar respostas", e);
        }
    }

    private Lesson exigirAulaAcessivel(UUID userId, UUID lessonId) {
        Lesson aula = lessons.findById(lessonId)
                .filter(Lesson::isPublished)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada"));
        var modulo = modules.findById(aula.getModuleId())
                .orElseThrow(() -> new NotFoundException("Aula não encontrada"));
        if (!acesso.cursosAcessiveis(userId).contains(modulo.getCourseId())) {
            throw new NotFoundException("Aula não encontrada");
        }
        return aula;
    }
}
