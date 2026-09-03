package com.betobanco.contests.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A juncao que a secao 07 exige.
 *
 * <p>"Um concurso pode pertencer a mais de uma carreira -- regra obrigatoria
 * mantida, e e ela que exige a tabela de juncao da secao 18."
 *
 * <p>Sem ela, um concurso do Banco do Brasil so caberia em "Bancaria", e o
 * cargo de tecnologia dele sumiria de quem estuda para Administrativa — que e
 * exatamente o aluno que mais precisa encontrar essa vaga.
 *
 * <p>Entidade propria em vez de {@code @ManyToMany}: a listagem filtra por
 * carreira com um EXISTS, e para isso a juncao precisa ser consultavel sozinha.
 */
@Entity
@Table(name = "contest_careers")
@IdClass(ContestCareer.Chave.class)
public class ContestCareer {

    @Id
    @Column(name = "contest_id")
    private UUID contestId;

    @Id
    @Column(name = "career_id")
    private UUID careerId;

    protected ContestCareer() {
    }

    public ContestCareer(UUID contestId, UUID careerId) {
        this.contestId = contestId;
        this.careerId = careerId;
    }

    public UUID getContestId() {
        return contestId;
    }

    public UUID getCareerId() {
        return careerId;
    }

    public static class Chave implements Serializable {
        private UUID contestId;
        private UUID careerId;

        public Chave() {
        }

        public Chave(UUID contestId, UUID careerId) {
            this.contestId = contestId;
            this.careerId = careerId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Chave outra)) {
                return false;
            }
            return Objects.equals(contestId, outra.contestId)
                    && Objects.equals(careerId, outra.careerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contestId, careerId);
        }
    }
}
