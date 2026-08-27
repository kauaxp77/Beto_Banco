package com.betobanco.courses.service;

import com.betobanco.courses.entity.Course;
import com.betobanco.courses.entity.CourseModule;
import com.betobanco.courses.entity.Lesson;
import com.betobanco.courses.entity.LessonComment;
import com.betobanco.courses.repository.CourseModuleRepository;
import com.betobanco.courses.repository.CourseRepository;
import com.betobanco.courses.repository.LessonCommentRepository;
import com.betobanco.courses.repository.LessonRepository;
import com.betobanco.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Lookups com 404 padronizado para a gestao de cursos. Vive no service para
 * o controller nunca ter um metodo que retorne {@code @Entity} — regra
 * ArchUnit {@code nenhumControllerRetornaEntidadeJpa}.
 */
@Service
public class AdminCourseLookup {

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final LessonCommentRepository comments;

    public AdminCourseLookup(CourseRepository courses, CourseModuleRepository modules,
                             LessonRepository lessons, LessonCommentRepository comments) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.comments = comments;
    }

    public Course curso(UUID id) {
        return courses.findById(id)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado"));
    }

    public CourseModule modulo(UUID id) {
        return modules.findById(id)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado"));
    }

    public Lesson aula(UUID id) {
        return lessons.findById(id)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada"));
    }

    public LessonComment comentario(UUID id) {
        return comments.findById(id)
                .orElseThrow(() -> new NotFoundException("Comentário não encontrado"));
    }
}
