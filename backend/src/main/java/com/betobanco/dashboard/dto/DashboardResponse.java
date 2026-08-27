package com.betobanco.dashboard.dto;

public record DashboardResponse(
        long totalAlunos,
        long alunosBloqueados,
        long produtosAtivos,
        long entitlementsAtivos,
        long pagamentosAprovados,
        long receitaAprovadaCents,
        long webhooksAguardandoAtencao) {
}
