package com.betobanco.contests.service;

import com.betobanco.shared.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Busca unificada. Documento Mestre V4.0, secao 07.
 *
 * <p>"Busca unica sobre concursos, cursos e posts; filtros por carreira, orgao,
 * banca, escolaridade, faixa salarial e status do edital; ordenacao por
 * relevancia ou data de inscricao; tolerancia a acento e erro de digitacao.
 * Implementacao: tsvector + pg_trgm no PostgreSQL -- sem servico externo ate 50
 * mil documentos."
 *
 * <p>A relevancia soma duas medidas porque elas erram em direcoes opostas:
 * {@code ts_rank_cd} entende radical e proximidade de termos mas exige palavra
 * inteira depois do stemming, e {@code similarity} do trigrama perdoa o erro de
 * digitacao mas nao sabe que "bancario" e "bancos" tem a mesma raiz. Uma
 * sozinha deixa metade das buscas sem resposta.
 */
@Service
public class SearchService {

    /**
     * Limiar do trigrama. Abaixo disso o operador {@code %} nao casa — e o
     * valor padrao do PostgreSQL (0.3) e alto demais para nome curto: "BB"
     * contra "Banco do Brasil" fica bem abaixo. Baixamos por consulta, com
     * SET LOCAL, para nao alterar o comportamento global do banco.
     */
    private static final String LIMIAR_TRIGRAMA = "0.15";

    private final EntityManager em;

    public SearchService(EntityManager em) {
        this.em = em;
    }

    public record Resultado(String kind, UUID id, String slug, String title, String subtitle,
                            String status, Long salaryCents, String educationLevel,
                            double relevance) {
    }

    public record Filtros(String tipo, UUID carreira, UUID orgao, String banca,
                          String escolaridade, Long salarioMin, Long salarioMax, String status) {

        public static Filtros nenhum() {
            return new Filtros(null, null, null, null, null, null, null, null);
        }
    }

    @Transactional(readOnly = true)
    public List<Resultado> buscar(String termo, Filtros filtros, String ordenarPor, int limite) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }

        em.createNativeQuery("SET LOCAL pg_trgm.similarity_threshold = " + LIMIAR_TRIGRAMA)
                .executeUpdate();

        // "data" ordena por prazo de inscricao (o que vence primeiro importa
        // mais); o padrao e relevancia. A clausula e escolhida entre duas
        // constantes -- nunca montada a partir da entrada do usuario.
        String ordenacao = "data".equalsIgnoreCase(ordenarPor)
                ? "s.sort_date ASC NULLS LAST, s.title"
                : "relevance DESC, s.title";

        Query consulta = em.createNativeQuery("""
                SELECT s.kind, s.id, s.slug, s.title, s.subtitle, s.status,
                       s.salary_cents, s.education_level,
                       ts_rank_cd(s.search_vector,
                                  plainto_tsquery('portuguese', imutavel_unaccent(:termo)))
                         + similarity(s.title_unaccented, imutavel_unaccent(:termo)) AS relevance
                  FROM vw_search s
                  LEFT JOIN contest_careers cc ON cc.contest_id = s.id
                 WHERE s.tenant_id = :tenantId
                   AND s.published
                   AND (s.search_vector @@ plainto_tsquery('portuguese', imutavel_unaccent(:termo))
                        OR s.title_unaccented %% imutavel_unaccent(:termo))
                   AND (CAST(:tipo AS text) IS NULL OR s.kind = :tipo)
                   AND (CAST(:carreira AS uuid) IS NULL OR cc.career_id = :carreira)
                   AND (CAST(:orgao AS uuid) IS NULL OR s.agency_id = :orgao)
                   AND (CAST(:banca AS text) IS NULL OR upper(s.board) = upper(:banca))
                   AND (CAST(:escolaridade AS text) IS NULL OR s.education_level = :escolaridade)
                   AND (CAST(:salarioMin AS bigint) IS NULL OR s.salary_cents >= :salarioMin)
                   AND (CAST(:salarioMax AS bigint) IS NULL OR s.salary_cents <= :salarioMax)
                   AND (CAST(:status AS text) IS NULL OR s.status = :status)
                 GROUP BY s.kind, s.id, s.slug, s.title, s.subtitle, s.status,
                          s.salary_cents, s.education_level, s.search_vector,
                          s.title_unaccented, s.sort_date
                 ORDER BY %s
                 LIMIT :limite
                """.formatted(ordenacao));

        consulta.setParameter("termo", termo);
        consulta.setParameter("tenantId", TenantContext.atual());
        consulta.setParameter("tipo", filtros.tipo());
        consulta.setParameter("carreira", filtros.carreira());
        consulta.setParameter("orgao", filtros.orgao());
        consulta.setParameter("banca", filtros.banca());
        consulta.setParameter("escolaridade", filtros.escolaridade());
        consulta.setParameter("salarioMin", filtros.salarioMin());
        consulta.setParameter("salarioMax", filtros.salarioMax());
        consulta.setParameter("status", filtros.status());
        consulta.setParameter("limite", Math.min(Math.max(limite, 1), 100));

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = consulta.getResultList();

        List<Resultado> resultados = new ArrayList<>(linhas.size());
        for (Object[] l : linhas) {
            resultados.add(new Resultado(
                    (String) l[0],
                    (UUID) l[1],
                    (String) l[2],
                    (String) l[3],
                    (String) l[4],
                    (String) l[5],
                    l[6] == null ? null : ((Number) l[6]).longValue(),
                    (String) l[7],
                    ((Number) l[8]).doubleValue()));
        }
        return resultados;
    }
}
