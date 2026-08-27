package com.betobanco.courses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(nullable = false)
    private String statement;

    /** Array JSON de alternativas, na ordem de exibicao. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String options;

    @Column(name = "correct_index", nullable = false)
    private int correctIndex;

    private String explanation;

    @Column(nullable = false)
    private int position;

    protected QuizQuestion() {
    }

    public QuizQuestion(UUID lessonId, String statement, String options, int correctIndex,
                        String explanation, int position) {
        this.lessonId = lessonId;
        this.statement = statement;
        this.options = options;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public void setCorrectIndex(int correctIndex) {
        this.correctIndex = correctIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
