package com.betobanco.courses.dto;

/** Gamificacao leve: constancia de estudo derivada do progresso real. */
public record StudyStatsResponse(
        int currentStreak,
        int bestStreak,
        int activeDaysLast30,
        boolean studiedToday) {
}
