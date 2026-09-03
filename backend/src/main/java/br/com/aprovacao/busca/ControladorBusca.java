package br.com.aprovacao.busca;

import br.com.aprovacao.common.PaginaCursor;
import br.com.aprovacao.config.FiltroTenant;
import br.com.aprovacao.config.PropriedadesPlataforma;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secao 07 -- Busca.
 *
 * <p>"Busca unica sobre concursos, cursos e posts; filtros por carreira, orgao,
 * banca, escolaridade, faixa salarial e status do edital; ordenacao por relevancia
 * ou data de inscricao; tolerancia a acento e erro de digitacao."
 *
 * <p>A relevancia soma duas medidas: ts_rank_cd sobre o tsvector, que entende
 * radical e proximidade de termos, e similarity() do trigrama, que e o que salva a
 * busca quando o aluno digita "cesgranio" ou "banco do brasl". Nenhuma das duas
 * sozinha cobre os dois casos.
 */
@RestController
@RequestMapping("/api/v1/busca")
@Tag(name = "Busca", description = "Busca unificada sobre concursos, cursos e posts (secao 07)")
public class ControladorBusca {

    private final EntityManager em;
    private final PropriedadesPlataforma props;

    public ControladorBusca(EntityManager em, PropriedadesPlataforma props) {
        this.em = em;
        this.props = props;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Busca unica com filtros e tolerancia a acento e erro de digitacao")
    public PaginaCursor<ResultadoBusca> buscar(
            @RequestParam(name = "q") String termo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String banca,
            @RequestParam(required = false) String escolaridade,
            @RequestParam(name = "salario_min", required = false) Long salarioMin,
            @RequestParam(name = "salario_max", required = false) Long salarioMax,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID carreira,
            @RequestParam(name = "ordenar_por", defaultValue = "relevancia") String ordenarPor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        int limite = PaginaCursor.normalizarLimite(limit);
        UUID tenantId = FiltroTenant.atual(props.tenantPadrao());
        String offsetCursor = PaginaCursor.decodificar(cursor);
        int offset = offsetCursor == null ? 0 : Integer.parseInt(offsetCursor);

        String ordenacao = "data".equals(ordenarPor)
                ? "b.data_ordenacao DESC NULLS LAST"
                : "relevancia DESC, b.titulo ASC";

        // Consulta parametrizada em todos os valores (secao 21). O unico trecho
        // interpolado e a clausula ORDER BY, escolhida entre duas constantes acima.
        Query consulta = em.createNativeQuery("""
                SELECT b.tipo, b.id, b.slug, b.titulo, b.subtitulo, b.status,
                       b.salario_centavos,
                       ts_rank_cd(b.busca, plainto_tsquery('portuguese', imutavel_unaccent(:termo)))
                         + similarity(b.titulo_sem_acento, imutavel_unaccent(:termo)) AS relevancia
                  FROM vw_busca b
                  LEFT JOIN concurso_carreira cc ON cc.concurso_id = b.id
                 WHERE b.tenant_id = :tenantId
                   AND (b.busca @@ plainto_tsquery('portuguese', imutavel_unaccent(:termo))
                        OR b.titulo_sem_acento %% imutavel_unaccent(:termo))
                   AND (CAST(:tipo AS text) IS NULL OR b.tipo = :tipo)
                   AND (CAST(:banca AS text) IS NULL OR b.subtitulo ILIKE '%%' || :banca || '%%')
                   AND (CAST(:escolaridade AS text) IS NULL OR b.escolaridade = :escolaridade)
                   AND (CAST(:salarioMin AS bigint) IS NULL OR b.salario_centavos >= :salarioMin)
                   AND (CAST(:salarioMax AS bigint) IS NULL OR b.salario_centavos <= :salarioMax)
                   AND (CAST(:status AS text) IS NULL OR b.status = :status)
                   AND (CAST(:carreira AS uuid) IS NULL OR cc.carreira_id = :carreira)
                 GROUP BY b.tipo, b.id, b.slug, b.titulo, b.subtitulo, b.status,
                          b.salario_centavos, b.busca, b.titulo_sem_acento, b.data_ordenacao
                 ORDER BY %s
                 LIMIT :limite OFFSET :deslocamento
                """.formatted(ordenacao));

        consulta.setParameter("termo", termo == null ? "" : termo);
        consulta.setParameter("tenantId", tenantId);
        consulta.setParameter("tipo", tipo);
        consulta.setParameter("banca", banca);
        consulta.setParameter("escolaridade", escolaridade);
        consulta.setParameter("salarioMin", salarioMin);
        consulta.setParameter("salarioMax", salarioMax);
        consulta.setParameter("status", status);
        consulta.setParameter("carreira", carreira);
        consulta.setParameter("limite", limite + 1);
        consulta.setParameter("deslocamento", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = consulta.getResultList();

        List<ResultadoBusca> itens = linhas.stream().map(l -> new ResultadoBusca(
                (String) l[0],
                (UUID) l[1],
                (String) l[2],
                (String) l[3],
                (String) l[4],
                (String) l[5],
                l[6] == null ? null : ((Number) l[6]).longValue(),
                ((Number) l[7]).doubleValue())).toList();

        // Busca com ranking nao tem chave estavel para cursor de valor -- a
        // relevancia muda a cada indexacao. Aqui o cursor carrega o deslocamento, e
        // o Base64 mantem o formato opaco como no resto da API.
        int finalOffset = offset;
        return PaginaCursor.de(itens, limite, ultimo -> String.valueOf(finalOffset + limite));
    }

    public record ResultadoBusca(
            String tipo, UUID id, String slug, String titulo, String subtitulo,
            String status, Long salarioCentavos, double relevancia) {}

    /** Mantido para leitura de contagem em consultas nativas que devolvem BigInteger. */
    static long paraLong(Object valor) {
        return valor instanceof BigInteger b ? b.longValue() : ((Number) valor).longValue();
    }
}
