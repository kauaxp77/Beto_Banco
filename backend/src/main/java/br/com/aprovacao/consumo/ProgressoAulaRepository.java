package br.com.aprovacao.consumo;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secao 18 -- progresso_aula tem chave primaria composta e e escrito com muita
 * frequencia (o player reporta a cada poucos segundos). Um UPSERT nativo evita o
 * par SELECT + INSERT/UPDATE, que sob concorrencia de duas abas do mesmo aluno
 * produziria violacao de chave.
 */
@Repository
public class ProgressoAulaRepository {

    private final EntityManager em;

    public ProgressoAulaRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * O cliente manda a posicao acumulada do player, nao o tempo desde o ultimo
     * envio. A CTE {@code anterior} le o valor antes da gravacao para que
     * dia_estudo (secao 09, sequencia de estudo) receba o incremento real -- somar
     * a posicao acumulada a cada chamada inflaria as horas do dia varias vezes.
     */
    @Transactional
    public void registrar(UUID usuarioId, UUID aulaId, int segundosVistos, boolean concluido) {
        em.createNativeQuery("""
                WITH anterior AS (
                    SELECT COALESCE((SELECT segundos_vistos
                                       FROM progresso_aula
                                      WHERE usuario_id = :usuarioId AND aula_id = :aulaId), 0) AS segundos
                ),
                gravado AS (
                    INSERT INTO progresso_aula (usuario_id, aula_id, segundos_vistos, concluido_em, atualizado_em)
                    VALUES (:usuarioId, :aulaId, :segundos, CASE WHEN :concluido THEN now() END, now())
                    ON CONFLICT (usuario_id, aula_id) DO UPDATE SET
                        -- Nunca retrocede: rebobinar o video nao pode apagar o progresso.
                        segundos_vistos = GREATEST(progresso_aula.segundos_vistos, EXCLUDED.segundos_vistos),
                        concluido_em    = COALESCE(progresso_aula.concluido_em, EXCLUDED.concluido_em),
                        atualizado_em   = now()
                    RETURNING segundos_vistos
                )
                INSERT INTO dia_estudo (usuario_id, dia, segundos)
                SELECT :usuarioId,
                       (now() AT TIME ZONE 'America/Sao_Paulo')::date,
                       GREATEST(gravado.segundos_vistos - anterior.segundos, 0)
                  FROM gravado, anterior
                ON CONFLICT (usuario_id, dia) DO UPDATE SET
                    segundos = dia_estudo.segundos + EXCLUDED.segundos
                """)
                .setParameter("usuarioId", usuarioId)
                .setParameter("aulaId", aulaId)
                .setParameter("segundos", Math.max(0, segundosVistos))
                .setParameter("concluido", concluido)
                .executeUpdate();
    }
}
