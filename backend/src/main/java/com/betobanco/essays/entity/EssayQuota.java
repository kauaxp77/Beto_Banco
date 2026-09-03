package com.betobanco.essays.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Cota de correcoes de um aluno em uma competencia. Secao 14.
 *
 * <p>O documento explica por que ela existe, e o motivo nao e burocratico:
 * "Correcao ilimitada destroi a margem: 4 correcoes mensais por 12 meses custam
 * R$ 864 por aluno de mentoria contra um ticket de R$ 3.564. A cota e o que
 * mantem a margem da secao 04 de pe."
 *
 * <p>A competencia e mensal e nao acumula. Acumular deixaria um aluno inativo
 * por seis meses despejar 24 correcoes de uma vez na fila, estourando o prazo
 * de 7 dias de todo mundo.
 */
@Entity
@Table(name = "essay_quotas")
@IdClass(EssayQuota.Chave.class)
public class EssayQuota {

    /** Fuso da plataforma: a virada do mes e a de Sao Paulo, nao a de UTC. */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "period")
    private LocalDate period;

    @Column(nullable = false)
    private short available;

    @Column(nullable = false)
    private short used;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected EssayQuota() {
    }

    public EssayQuota(UUID userId, LocalDate period, int available) {
        this.userId = userId;
        this.period = period;
        this.available = (short) available;
    }

    /** Primeiro dia do mes corrente — a chave da competencia. */
    public static LocalDate competenciaAtual() {
        return LocalDate.now(FUSO).withDayOfMonth(1);
    }

    public int restantes() {
        return available - used;
    }

    public boolean temSaldo() {
        return restantes() > 0;
    }

    /**
     * Consome uma correcao. O CHECK do banco (used <= available) e a garantia
     * real contra dois envios simultaneos furarem a cota; este metodo so evita
     * o round-trip no caso comum.
     */
    public void consumir() {
        if (!temSaldo()) {
            throw new IllegalStateException("Cota de correções esgotada nesta competência.");
        }
        this.used++;
        this.updatedAt = Instant.now();
    }

    /** Compra avulsa ou concessao do admin somam a cota da competencia corrente. */
    public void acrescentar(int quantidade) {
        this.available += (short) quantidade;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public int getAvailable() {
        return available;
    }

    public int getUsed() {
        return used;
    }

    /** Chave composta (aluno, competencia). */
    public static class Chave implements Serializable {
        private UUID userId;
        private LocalDate period;

        public Chave() {
        }

        public Chave(UUID userId, LocalDate period) {
            this.userId = userId;
            this.period = period;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Chave outra)) {
                return false;
            }
            return Objects.equals(userId, outra.userId) && Objects.equals(period, outra.period);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, period);
        }
    }
}
